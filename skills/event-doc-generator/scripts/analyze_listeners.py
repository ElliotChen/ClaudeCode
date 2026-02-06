#!/usr/bin/env python3
"""
Listener 分析器 - 找出所有監聽 Event 的程式碼
"""

import re
import json
import argparse
from pathlib import Path
from typing import List, Dict, Optional
from dataclasses import dataclass, asdict


@dataclass
class ListenerInfo:
    """Listener 資訊"""
    event_name: str
    class_name: str
    method_name: str
    file_path: str
    line_number: int
    package: str = ""
    is_async: bool = False
    is_transactional: bool = False
    transaction_phase: Optional[str] = None
    condition: Optional[str] = None
    javadoc: Optional[str] = None


class ListenerAnalyzer:
    """分析 Event Listener"""
    
    def __init__(self, source_dir: Path):
        self.source_dir = Path(source_dir)
        self.listeners: List[ListenerInfo] = []
    
    def analyze(self) -> List[ListenerInfo]:
        """分析所有 Listener"""
        print(f"🔍 分析 Event Listener...")
        
        # 掃描所有 Java 檔案
        java_files = list(self.source_dir.rglob("*.java"))
        
        for java_file in java_files:
            self._analyze_file(java_file)
        
        print(f"✅ 找到 {len(self.listeners)} 個 Listener")
        return self.listeners
    
    def _analyze_file(self, file_path: Path):
        """分析單一檔案"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # 提取 package
            package = self._extract_package(content)
            
            # 提取類別名稱
            class_name = self._extract_class_name(content)
            
            # 檢查是否有 @Async
            has_class_async = '@Async' in content
            
            # 尋找 @EventListener 和 @TransactionalEventListener
            self._find_listeners(content, file_path, package, class_name, has_class_async)
            
        except Exception as e:
            print(f"⚠️  分析 {file_path} 時發生錯誤: {e}")
    
    def _extract_package(self, content: str) -> str:
        """提取 package"""
        match = re.search(r'package\s+([\w.]+);', content)
        return match.group(1) if match else ""
    
    def _extract_class_name(self, content: str) -> str:
        """提取類別名稱"""
        match = re.search(r'(?:public\s+)?(?:class|interface)\s+(\w+)', content)
        return match.group(1) if match else "Unknown"
    
    def _find_listeners(self, content: str, file_path: Path, package: str, 
                       class_name: str, has_class_async: bool):
        """尋找 Event Listener"""
        lines = content.split('\n')
        
        i = 0
        while i < len(lines):
            line = lines[i]
            
            # 檢查是否有 @EventListener 或 @TransactionalEventListener
            if '@EventListener' in line or '@TransactionalEventListener' in line:
                listener_info = self._parse_listener(
                    lines, i, file_path, package, class_name, has_class_async
                )
                if listener_info:
                    self.listeners.append(listener_info)
            
            i += 1
    
    def _parse_listener(self, lines: List[str], start_line: int, file_path: Path,
                       package: str, class_name: str, has_class_async: bool) -> Optional[ListenerInfo]:
        """解析單一 Listener"""
        # 收集註解資訊
        is_transactional = '@TransactionalEventListener' in lines[start_line]
        transaction_phase = None
        condition = None
        is_async = has_class_async  # 繼承類別層級的 @Async
        
        # 解析註解參數
        annotation_line = lines[start_line]
        
        if is_transactional:
            # 提取 phase
            phase_match = re.search(r'phase\s*=\s*TransactionPhase\.(\w+)', annotation_line)
            if phase_match:
                transaction_phase = phase_match.group(1)
        
        # 提取 condition
        condition_match = re.search(r'condition\s*=\s*"([^"]+)"', annotation_line)
        if condition_match:
            condition = condition_match.group(1)
        
        # 檢查方法是否有 @Async
        i = start_line + 1
        while i < len(lines) and not lines[i].strip().startswith('public'):
            if '@Async' in lines[i]:
                is_async = True
            i += 1
        
        # 找出方法宣告
        method_line = None
        for j in range(start_line + 1, min(start_line + 10, len(lines))):
            if re.search(r'(public|private|protected)\s+\w+\s+\w+\s*\(', lines[j]):
                method_line = j
                break
        
        if not method_line:
            return None
        
        # 提取方法名稱
        method_match = re.search(r'(public|private|protected)\s+\w+\s+(\w+)\s*\(', lines[method_line])
        if not method_match:
            return None
        
        method_name = method_match.group(2)
        
        # 提取 Event 型別（從參數推斷）
        event_name = self._extract_event_from_parameter(lines[method_line])
        
        # 如果參數沒有 Event，嘗試從註解的 classes 屬性取得
        if not event_name:
            classes_match = re.search(r'classes\s*=\s*\{?\s*(\w+Event)\.class', annotation_line)
            if classes_match:
                event_name = classes_match.group(1)
            else:
                # 嘗試單一 class
                class_match = re.search(r'@\w+EventListener\s*\(\s*(\w+Event)\.class', annotation_line)
                if class_match:
                    event_name = class_match.group(1)
        
        if not event_name:
            return None
        
        # 提取 JavaDoc
        javadoc = self._extract_javadoc(lines, start_line)
        
        return ListenerInfo(
            event_name=event_name,
            class_name=class_name,
            method_name=method_name,
            file_path=str(file_path.relative_to(self.source_dir)),
            line_number=start_line + 1,
            package=package,
            is_async=is_async,
            is_transactional=is_transactional,
            transaction_phase=transaction_phase,
            condition=condition,
            javadoc=javadoc
        )
    
    def _extract_event_from_parameter(self, method_line: str) -> Optional[str]:
        """從方法參數提取 Event 型別"""
        # 找出參數部分
        param_match = re.search(r'\(([^)]+)\)', method_line)
        if not param_match:
            return None
        
        params = param_match.group(1)
        
        # 找出以 Event 結尾的型別
        event_match = re.search(r'(\w+Event)\s+\w+', params)
        if event_match:
            return event_match.group(1)
        
        return None
    
    def _extract_javadoc(self, lines: List[str], line_num: int) -> Optional[str]:
        """提取 JavaDoc"""
        # 往前找 JavaDoc
        javadoc_lines = []
        in_javadoc = False
        
        for i in range(line_num - 1, max(0, line_num - 20), -1):
            line = lines[i].strip()
            
            if line == '*/':
                in_javadoc = True
                continue
            
            if line.startswith('/**'):
                break
            
            if in_javadoc:
                if line.startswith('*'):
                    line = line[1:].strip()
                if line and not line.startswith('@'):
                    javadoc_lines.insert(0, line)
        
        return ' '.join(javadoc_lines).strip() if javadoc_lines else None
    
    def save_to_json(self, output_file: Path):
        """儲存分析結果"""
        data = {
            'listeners': [asdict(listener) for listener in self.listeners],
            'summary': {
                'total_listeners': len(self.listeners),
                'async_listeners': sum(1 for l in self.listeners if l.is_async),
                'transactional_listeners': sum(1 for l in self.listeners if l.is_transactional),
                'events_listened': list(set(l.event_name for l in self.listeners)),
            }
        }
        
        output_file.parent.mkdir(parents=True, exist_ok=True)
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
        
        print(f"💾 Listener 分析結果已儲存至: {output_file}")
    
    def group_by_event(self) -> Dict[str, List[ListenerInfo]]:
        """依 Event 分組"""
        grouped = {}
        for listener in self.listeners:
            if listener.event_name not in grouped:
                grouped[listener.event_name] = []
            grouped[listener.event_name].append(listener)
        return grouped


def main():
    parser = argparse.ArgumentParser(description='分析 Event Listener')
    parser.add_argument('--source-dir', type=str, required=True,
                       help='Java 原始碼目錄')
    parser.add_argument('--output', type=str, default='listener-analysis.json',
                       help='輸出 JSON 檔案')
    
    args = parser.parse_args()
    
    analyzer = ListenerAnalyzer(Path(args.source_dir))
    listeners = analyzer.analyze()
    analyzer.save_to_json(Path(args.output))
    
    # 顯示摘要
    print("\n📊 Listener 分析摘要:")
    print(f"   總 Listener 數: {len(listeners)}")
    print(f"   異步 Listener: {sum(1 for l in listeners if l.is_async)}")
    print(f"   事務性 Listener: {sum(1 for l in listeners if l.is_transactional)}")
    
    grouped = analyzer.group_by_event()
    print(f"\n   監聽的 Event 種類: {len(grouped)}")
    for event_name, lstnrs in sorted(grouped.items()):
        print(f"   - {event_name}: {len(lstnrs)} 個 Listener")


if __name__ == '__main__':
    main()
