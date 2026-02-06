#!/usr/bin/env python3
"""
Publisher 分析器 - 找出所有發送 Event 的程式碼
"""

import re
import json
import argparse
from pathlib import Path
from typing import List, Dict, Optional
from dataclasses import dataclass, asdict


@dataclass
class PublisherInfo:
    """Publisher 資訊"""
    event_name: str
    class_name: str
    method_name: str
    file_path: str
    line_number: int
    code_snippet: str
    package: str = ""


class PublisherAnalyzer:
    """分析 Event Publisher"""
    
    def __init__(self, source_dir: Path):
        self.source_dir = Path(source_dir)
        self.publishers: List[PublisherInfo] = []
    
    def analyze(self) -> List[PublisherInfo]:
        """分析所有 Publisher"""
        print(f"🔍 分析 Event Publisher...")
        
        # 掃描所有 Java 檔案
        java_files = list(self.source_dir.rglob("*.java"))
        
        for java_file in java_files:
            self._analyze_file(java_file)
        
        print(f"✅ 找到 {len(self.publishers)} 個 Publisher")
        return self.publishers
    
    def _analyze_file(self, file_path: Path):
        """分析單一檔案"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # 提取 package
            package = self._extract_package(content)
            
            # 提取類別名稱
            class_name = self._extract_class_name(content)
            
            # 尋找 publishEvent 呼叫
            self._find_publish_calls(content, file_path, package, class_name)
            
        except Exception as e:
            print(f"⚠️  分析 {file_path} 時發生錯誤: {e}")
    
    def _extract_package(self, content: str) -> str:
        """提取 package"""
        match = re.search(r'package\s+([\w.]+);', content)
        return match.group(1) if match else ""
    
    def _extract_class_name(self, content: str) -> str:
        """提取類別名稱"""
        match = re.search(r'(?:public\s+)?(?:class|interface|record)\s+(\w+)', content)
        return match.group(1) if match else "Unknown"
    
    def _find_publish_calls(self, content: str, file_path: Path, package: str, class_name: str):
        """尋找 publishEvent 呼叫"""
        lines = content.split('\n')
        
        # Pattern 1: applicationEventPublisher.publishEvent(new XxxEvent(...))
        # Pattern 2: eventPublisher.publish(new XxxEvent(...))
        # Pattern 3: publishEvent(new XxxEvent(...))
        
        patterns = [
            r'\.publishEvent\s*\(\s*new\s+(\w+Event)',
            r'\.publish\s*\(\s*new\s+(\w+Event)',
            r'publishEvent\s*\(\s*new\s+(\w+Event)',
        ]
        
        for line_num, line in enumerate(lines, start=1):
            for pattern in patterns:
                match = re.search(pattern, line)
                if match:
                    event_name = match.group(1)
                    
                    # 找出所在的方法
                    method_name = self._find_method_name(lines, line_num)
                    
                    # 取得程式碼片段
                    code_snippet = self._get_code_snippet(lines, line_num)
                    
                    self.publishers.append(PublisherInfo(
                        event_name=event_name,
                        class_name=class_name,
                        method_name=method_name,
                        file_path=str(file_path.relative_to(self.source_dir)),
                        line_number=line_num,
                        code_snippet=code_snippet,
                        package=package
                    ))
    
    def _find_method_name(self, lines: List[str], target_line: int) -> str:
        """往回找出所在的方法名稱"""
        # 從目標行往回找
        for i in range(target_line - 1, -1, -1):
            line = lines[i].strip()
            
            # 找到方法宣告
            match = re.search(r'(?:public|private|protected)?\s*(?:static\s+)?(?:\w+\s+)*(\w+)\s*\(', line)
            if match:
                return match.group(1)
        
        return "unknown"
    
    def _get_code_snippet(self, lines: List[str], line_num: int, context: int = 2) -> str:
        """取得程式碼片段（包含上下文）"""
        start = max(0, line_num - context - 1)
        end = min(len(lines), line_num + context)
        
        snippet_lines = []
        for i in range(start, end):
            prefix = ">>> " if i == line_num - 1 else "    "
            snippet_lines.append(f"{prefix}{lines[i]}")
        
        return '\n'.join(snippet_lines)
    
    def save_to_json(self, output_file: Path):
        """儲存分析結果"""
        data = {
            'publishers': [asdict(pub) for pub in self.publishers],
            'summary': {
                'total_publishers': len(self.publishers),
                'events_published': list(set(p.event_name for p in self.publishers)),
            }
        }
        
        output_file.parent.mkdir(parents=True, exist_ok=True)
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
        
        print(f"💾 Publisher 分析結果已儲存至: {output_file}")
    
    def group_by_event(self) -> Dict[str, List[PublisherInfo]]:
        """依 Event 分組"""
        grouped = {}
        for pub in self.publishers:
            if pub.event_name not in grouped:
                grouped[pub.event_name] = []
            grouped[pub.event_name].append(pub)
        return grouped


def main():
    parser = argparse.ArgumentParser(description='分析 Event Publisher')
    parser.add_argument('--source-dir', type=str, required=True,
                       help='Java 原始碼目錄')
    parser.add_argument('--output', type=str, default='publisher-analysis.json',
                       help='輸出 JSON 檔案')
    
    args = parser.parse_args()
    
    analyzer = PublisherAnalyzer(Path(args.source_dir))
    publishers = analyzer.analyze()
    analyzer.save_to_json(Path(args.output))
    
    # 顯示摘要
    print("\n📊 Publisher 分析摘要:")
    print(f"   總 Publisher 數: {len(publishers)}")
    
    grouped = analyzer.group_by_event()
    print(f"\n   發布的 Event 種類: {len(grouped)}")
    for event_name, pubs in sorted(grouped.items()):
        print(f"   - {event_name}: {len(pubs)} 個 Publisher")


if __name__ == '__main__':
    main()
