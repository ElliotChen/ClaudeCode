#!/usr/bin/env python3
"""
Event 掃描器 - 掃描 Spring Boot 專案中的所有 Event 類別
"""

import os
import re
import json
import argparse
from pathlib import Path
from typing import List, Dict, Optional
from dataclasses import dataclass, asdict


@dataclass
class FieldInfo:
    """Event 欄位資訊"""
    name: str
    type: str
    javadoc: Optional[str] = None
    annotations: List[str] = None
    
    def __post_init__(self):
        if self.annotations is None:
            self.annotations = []


@dataclass
class EventInfo:
    """Event 類別資訊"""
    name: str
    package: str
    file_path: str
    bounded_context: str
    javadoc: Optional[str] = None
    fields: List[FieldInfo] = None
    extends: Optional[str] = None
    annotations: List[str] = None
    is_record: bool = False
    
    def __post_init__(self):
        if self.fields is None:
            self.fields = []
        if self.annotations is None:
            self.annotations = []


class EventScanner:
    """掃描 Java 原始碼中的 Event 類別"""
    
    # Event 類別的判定模式
    EVENT_PATTERNS = [
        r'class\s+\w+Event\s+extends',  # 繼承 Event 的類別
        r'class\s+\w+Event\s+implements',  # 實作介面的 Event
        r'record\s+\w+Event\s*\(',  # Record 類型的 Event
        r'@DomainEvent',  # 標記 @DomainEvent 的類別
    ]
    
    def __init__(self, source_dir: Path):
        self.source_dir = Path(source_dir)
        self.events: List[EventInfo] = []
    
    def scan(self) -> List[EventInfo]:
        """掃描所有 Event 類別"""
        print(f"📂 掃描目錄: {self.source_dir}")
        
        # 遞迴掃描所有 .java 檔案
        java_files = list(self.source_dir.rglob("*.java"))
        print(f"📄 找到 {len(java_files)} 個 Java 檔案")
        
        for java_file in java_files:
            # 只處理 event 相關的套件
            if self._is_event_package(java_file):
                event_info = self._parse_event_file(java_file)
                if event_info:
                    self.events.append(event_info)
        
        print(f"✅ 找到 {len(self.events)} 個 Event 類別")
        return self.events
    
    def _is_event_package(self, file_path: Path) -> bool:
        """判斷檔案是否在 event 相關的套件中"""
        path_str = str(file_path)
        return any(pattern in path_str.lower() for pattern in [
            '/event/', '/events/', 'domain/event', 'domain/events'
        ])
    
    def _parse_event_file(self, file_path: Path) -> Optional[EventInfo]:
        """解析單一 Event 檔案"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # 檢查是否為 Event 類別
            if not self._is_event_class(content):
                return None
            
            # 提取 package
            package = self._extract_package(content)
            
            # 提取類別名稱
            class_name = self._extract_class_name(content)
            
            if not class_name:
                return None
            
            # 提取 JavaDoc
            javadoc = self._extract_class_javadoc(content, class_name)
            
            # 判斷是否為 Record
            is_record = 'record ' + class_name in content
            
            # 提取欄位
            fields = self._extract_fields(content, is_record)
            
            # 提取繼承資訊
            extends = self._extract_extends(content, class_name)
            
            # 提取註解
            annotations = self._extract_class_annotations(content, class_name)
            
            # 推斷 Bounded Context
            bounded_context = self._infer_bounded_context(package)
            
            return EventInfo(
                name=class_name,
                package=package,
                file_path=str(file_path.relative_to(self.source_dir)),
                bounded_context=bounded_context,
                javadoc=javadoc,
                fields=fields,
                extends=extends,
                annotations=annotations,
                is_record=is_record
            )
            
        except Exception as e:
            print(f"⚠️  解析 {file_path} 時發生錯誤: {e}")
            return None
    
    def _is_event_class(self, content: str) -> bool:
        """判斷是否為 Event 類別"""
        return any(re.search(pattern, content) for pattern in self.EVENT_PATTERNS)
    
    def _extract_package(self, content: str) -> str:
        """提取 package 宣告"""
        match = re.search(r'package\s+([\w.]+);', content)
        return match.group(1) if match else ""
    
    def _extract_class_name(self, content: str) -> Optional[str]:
        """提取類別名稱"""
        # 嘗試匹配 class
        match = re.search(r'(?:public\s+)?(?:final\s+)?class\s+(\w+)', content)
        if match:
            return match.group(1)
        
        # 嘗試匹配 record
        match = re.search(r'(?:public\s+)?record\s+(\w+)', content)
        if match:
            return match.group(1)
        
        return None
    
    def _extract_class_javadoc(self, content: str, class_name: str) -> Optional[str]:
        """提取類別的 JavaDoc"""
        # 找到類別宣告的位置
        class_pattern = rf'(?:class|record)\s+{class_name}'
        class_match = re.search(class_pattern, content)
        
        if not class_match:
            return None
        
        # 往前找 JavaDoc
        before_class = content[:class_match.start()]
        javadoc_pattern = r'/\*\*(.*?)\*/'
        javadoc_matches = re.finditer(javadoc_pattern, before_class, re.DOTALL)
        
        # 取最後一個（最接近類別宣告的）
        javadoc_match = None
        for match in javadoc_matches:
            javadoc_match = match
        
        if javadoc_match:
            javadoc_text = javadoc_match.group(1)
            # 清理 JavaDoc 格式
            lines = []
            for line in javadoc_text.split('\n'):
                line = line.strip()
                if line.startswith('*'):
                    line = line[1:].strip()
                if line and not line.startswith('@'):
                    lines.append(line)
            return ' '.join(lines).strip()
        
        return None
    
    def _extract_fields(self, content: str, is_record: bool) -> List[FieldInfo]:
        """提取欄位資訊"""
        fields = []
        
        if is_record:
            # Record 的欄位在括號內
            match = re.search(r'record\s+\w+\s*\((.*?)\)', content, re.DOTALL)
            if match:
                params = match.group(1)
                for param in params.split(','):
                    param = param.strip()
                    if param:
                        # 解析型別和名稱
                        parts = param.split()
                        if len(parts) >= 2:
                            field_type = ' '.join(parts[:-1])
                            field_name = parts[-1]
                            fields.append(FieldInfo(
                                name=field_name,
                                type=field_type
                            ))
        else:
            # 一般類別的欄位
            field_pattern = r'private\s+(?:final\s+)?([\w<>,\s]+)\s+(\w+)\s*;'
            for match in re.finditer(field_pattern, content):
                field_type = match.group(1).strip()
                field_name = match.group(2)
                
                # 提取欄位的 JavaDoc
                field_javadoc = self._extract_field_javadoc(content, field_name)
                
                # 提取欄位的註解
                field_annotations = self._extract_field_annotations(content, field_name)
                
                fields.append(FieldInfo(
                    name=field_name,
                    type=field_type,
                    javadoc=field_javadoc,
                    annotations=field_annotations
                ))
        
        return fields
    
    def _extract_field_javadoc(self, content: str, field_name: str) -> Optional[str]:
        """提取欄位的 JavaDoc"""
        # 找到欄位宣告
        field_pattern = rf'(\w+)\s+{field_name}\s*;'
        field_match = re.search(field_pattern, content)
        
        if not field_match:
            return None
        
        # 往前找 JavaDoc
        before_field = content[:field_match.start()]
        javadoc_pattern = r'/\*\*(.*?)\*/'
        javadoc_matches = re.finditer(javadoc_pattern, before_field, re.DOTALL)
        
        javadoc_match = None
        for match in javadoc_matches:
            javadoc_match = match
        
        if javadoc_match:
            javadoc_text = javadoc_match.group(1)
            lines = []
            for line in javadoc_text.split('\n'):
                line = line.strip()
                if line.startswith('*'):
                    line = line[1:].strip()
                if line and not line.startswith('@'):
                    lines.append(line)
            return ' '.join(lines).strip()
        
        return None
    
    def _extract_field_annotations(self, content: str, field_name: str) -> List[str]:
        """提取欄位的註解"""
        annotations = []
        field_pattern = rf'(@\w+(?:\([^)]*\))?)\s+(?:private\s+)?(?:final\s+)?[\w<>,\s]+\s+{field_name}\s*;'
        
        matches = re.finditer(field_pattern, content, re.MULTILINE)
        for match in matches:
            annotations.append(match.group(1))
        
        return annotations
    
    def _extract_extends(self, content: str, class_name: str) -> Optional[str]:
        """提取繼承資訊"""
        pattern = rf'class\s+{class_name}\s+extends\s+([\w<>]+)'
        match = re.search(pattern, content)
        return match.group(1) if match else None
    
    def _extract_class_annotations(self, content: str, class_name: str) -> List[str]:
        """提取類別註解"""
        annotations = []
        
        # 找到類別宣告位置
        class_pattern = rf'(?:class|record)\s+{class_name}'
        class_match = re.search(class_pattern, content)
        
        if not class_match:
            return annotations
        
        # 往前找註解（最多往前找 500 字元）
        start_pos = max(0, class_match.start() - 500)
        before_class = content[start_pos:class_match.start()]
        
        # 找出所有註解
        annotation_pattern = r'@(\w+)(?:\([^)]*\))?'
        for match in re.finditer(annotation_pattern, before_class):
            annotations.append(match.group(0))
        
        return annotations
    
    def _infer_bounded_context(self, package: str) -> str:
        """從 package 推斷 Bounded Context"""
        parts = package.split('.')
        
        # 嘗試從 package 結構推斷
        # 例如: com.example.user.domain.event -> User
        # 例如: com.example.order.event -> Order
        
        for i, part in enumerate(parts):
            if part in ['domain', 'event', 'events']:
                if i > 0:
                    return parts[i - 1].capitalize()
        
        # 如果找不到明確的 context，返回最後一個有意義的 package
        meaningful_parts = [p for p in parts if p not in ['com', 'org', 'net', 'domain', 'event', 'events']]
        return meaningful_parts[-1].capitalize() if meaningful_parts else 'Unknown'
    
    def save_to_json(self, output_file: Path):
        """儲存分析結果為 JSON"""
        data = {
            'events': [asdict(event) for event in self.events],
            'summary': {
                'total_events': len(self.events),
                'bounded_contexts': list(set(e.bounded_context for e in self.events)),
            }
        }
        
        output_file.parent.mkdir(parents=True, exist_ok=True)
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
        
        print(f"💾 分析結果已儲存至: {output_file}")


def main():
    parser = argparse.ArgumentParser(description='掃描 Spring Boot 專案中的 Event 類別')
    parser.add_argument('--source-dir', type=str, required=True,
                       help='Java 原始碼目錄 (例如: src/main/java)')
    parser.add_argument('--output', type=str, default='event-analysis.json',
                       help='輸出 JSON 檔案路徑')
    
    args = parser.parse_args()
    
    scanner = EventScanner(Path(args.source_dir))
    events = scanner.scan()
    scanner.save_to_json(Path(args.output))
    
    # 顯示摘要
    print("\n📊 掃描摘要:")
    print(f"   總 Event 數: {len(events)}")
    
    contexts = {}
    for event in events:
        contexts[event.bounded_context] = contexts.get(event.bounded_context, 0) + 1
    
    print("\n   依 Bounded Context 分布:")
    for context, count in sorted(contexts.items()):
        print(f"   - {context}: {count}")


if __name__ == '__main__':
    main()
