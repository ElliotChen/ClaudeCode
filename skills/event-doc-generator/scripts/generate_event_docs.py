#!/usr/bin/env python3
"""
Event 文件產生器 - 為每個 Event 生成獨立的 Markdown 文件
"""

import json
import argparse
from pathlib import Path
from typing import Dict, List, Optional
from datetime import datetime


class EventDocGenerator:
    """Event 文件產生器"""
    
    def __init__(self, events_file: Path, publishers_file: Path, listeners_file: Path):
        # 載入分析結果
        with open(events_file, 'r', encoding='utf-8') as f:
            self.events_data = json.load(f)
        
        with open(publishers_file, 'r', encoding='utf-8') as f:
            self.publishers_data = json.load(f)
        
        with open(listeners_file, 'r', encoding='utf-8') as f:
            self.listeners_data = json.load(f)
        
        # 建立索引
        self.events = {e['name']: e for e in self.events_data['events']}
        self.publishers_by_event = self._group_by_event(self.publishers_data['publishers'], 'event_name')
        self.listeners_by_event = self._group_by_event(self.listeners_data['listeners'], 'event_name')
    
    def _group_by_event(self, items: List[Dict], key: str) -> Dict[str, List[Dict]]:
        """將項目依 Event 分組"""
        grouped = {}
        for item in items:
            event_name = item[key]
            if event_name not in grouped:
                grouped[event_name] = []
            grouped[event_name].append(item)
        return grouped
    
    def generate_all(self, output_dir: Path):
        """產生所有 Event 的文件"""
        output_dir = Path(output_dir)
        events_dir = output_dir / 'events'
        events_dir.mkdir(parents=True, exist_ok=True)
        
        print(f"📝 開始產生 Event 文件...")
        
        for event_name, event_data in self.events.items():
            doc_path = events_dir / f"{event_name}.md"
            self._generate_event_doc(event_name, event_data, doc_path)
            print(f"   ✅ {event_name}.md")
        
        # 產生索引文件
        self._generate_readme(output_dir)
        self._generate_catalog(output_dir)
        self._generate_bounded_contexts(output_dir)
        
        print(f"\n✨ 所有文件已產生至: {output_dir}")
    
    def _generate_event_doc(self, event_name: str, event_data: Dict, output_path: Path):
        """產生單一 Event 的文件"""
        md = []
        
        # 標題
        md.append(f"# {event_name}\n")
        
        # 基本資訊
        md.append("## 📋 基本資訊\n")
        md.append(f"- **Package**: `{event_data['package']}`")
        md.append(f"- **Bounded Context**: {event_data['bounded_context']}")
        md.append(f"- **檔案路徑**: `{event_data['file_path']}`")
        
        if event_data['is_record']:
            md.append("- **類型**: Java Record")
        if event_data['extends']:
            md.append(f"- **繼承**: `{event_data['extends']}`")
        if event_data['annotations']:
            md.append(f"- **註解**: {', '.join(f'`{a}`' for a in event_data['annotations'])}")
        
        md.append("")
        
        # 說明
        if event_data.get('javadoc'):
            md.append("## 📖 說明\n")
            md.append(event_data['javadoc'])
            md.append("")
        
        # 欄位定義
        if event_data['fields']:
            md.append("## 🔧 欄位定義\n")
            md.append("| 欄位名稱 | 型別 | 說明 | 註解 |")
            md.append("|---------|------|------|------|")
            
            for field in event_data['fields']:
                field_name = field['name']
                field_type = f"`{field['type']}`"
                field_doc = field.get('javadoc', '-')
                field_annotations = ', '.join(f"`{a}`" for a in field.get('annotations', [])) or '-'
                
                md.append(f"| {field_name} | {field_type} | {field_doc} | {field_annotations} |")
            
            md.append("")
        
        # Publisher 資訊
        publishers = self.publishers_by_event.get(event_name, [])
        if publishers:
            md.append("## 📤 Publisher (事件發送者)\n")
            md.append(f"此事件由 **{len(publishers)}** 個地方發送:\n")
            
            for i, pub in enumerate(publishers, 1):
                md.append(f"### {i}. {pub['class_name']}.{pub['method_name']}()\n")
                md.append(f"- **檔案**: `{pub['file_path']}`")
                md.append(f"- **行號**: {pub['line_number']}")
                md.append(f"- **Package**: `{pub['package']}`\n")
                
                if pub.get('code_snippet'):
                    md.append("**程式碼片段**:")
                    md.append("```java")
                    md.append(pub['code_snippet'])
                    md.append("```\n")
        else:
            md.append("## 📤 Publisher (事件發送者)\n")
            md.append("⚠️ 目前沒有找到發送此事件的程式碼。\n")
        
        # Listener 資訊
        listeners = self.listeners_by_event.get(event_name, [])
        if listeners:
            md.append("## 📥 Listener (事件監聽者)\n")
            md.append(f"此事件被 **{len(listeners)}** 個 Listener 監聽:\n")
            
            for i, listener in enumerate(listeners, 1):
                md.append(f"### {i}. {listener['class_name']}.{listener['method_name']}()\n")
                md.append(f"- **檔案**: `{listener['file_path']}`")
                md.append(f"- **行號**: {listener['line_number']}")
                md.append(f"- **Package**: `{listener['package']}`")
                
                # 執行特性
                features = []
                if listener.get('is_async'):
                    features.append("🔄 異步執行")
                if listener.get('is_transactional'):
                    phase = listener.get('transaction_phase', 'AFTER_COMMIT')
                    features.append(f"🔒 事務性 ({phase})")
                
                if features:
                    md.append(f"- **執行特性**: {', '.join(features)}")
                
                if listener.get('condition'):
                    md.append(f"- **條件**: `{listener['condition']}`")
                
                if listener.get('javadoc'):
                    md.append(f"\n**說明**: {listener['javadoc']}")
                
                md.append("")
        else:
            md.append("## 📥 Listener (事件監聽者)\n")
            md.append("⚠️ 目前沒有找到監聽此事件的程式碼。\n")
        
        # 事件流程圖
        if publishers or listeners:
            md.append("## 📊 事件流程圖\n")
            md.append("```mermaid")
            md.append("sequenceDiagram")
            
            for pub in publishers[:3]:  # 最多顯示 3 個 Publisher
                pub_name = f"{pub['class_name']}"
                md.append(f"    participant {pub_name}")
            
            md.append(f"    participant {event_name}")
            
            for listener in listeners[:5]:  # 最多顯示 5 個 Listener
                listener_name = f"{listener['class_name']}"
                md.append(f"    participant {listener_name}")
            
            md.append("")
            
            for pub in publishers[:3]:
                pub_name = f"{pub['class_name']}"
                md.append(f"    {pub_name}->>+{event_name}: publish")
            
            for listener in listeners[:5]:
                listener_name = f"{listener['class_name']}"
                annotation = ""
                if listener.get('is_async'):
                    annotation = " (async)"
                elif listener.get('is_transactional'):
                    phase = listener.get('transaction_phase', 'AFTER_COMMIT')
                    annotation = f" ({phase})"
                
                md.append(f"    {event_name}->>+{listener_name}: handle{annotation}")
                md.append(f"    {listener_name}-->>-{event_name}: done")
            
            if publishers:
                md.append(f"    {event_name}-->>-{publishers[0]['class_name']}: completed")
            
            md.append("```\n")
        
        # 使用場景
        md.append("## 💡 使用場景\n")
        md.append("<!-- 請在此補充此事件的業務使用場景 -->\n")
        md.append("此事件通常在以下情況下觸發:\n")
        md.append("- TODO: 補充使用場景\n")
        
        # 相關事件
        md.append("## 🔗 相關事件\n")
        md.append("<!-- 請在此補充相關的其他事件 -->\n")
        md.append("- TODO: 補充相關事件\n")
        
        # 注意事項
        md.append("## ⚠️ 注意事項\n")
        md.append("<!-- 請在此補充開發者需要注意的事項 -->\n")
        md.append("- TODO: 補充注意事項\n")
        
        # Footer
        md.append("---\n")
        md.append(f"*文件自動產生時間: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}*")
        
        # 寫入檔案
        with open(output_path, 'w', encoding='utf-8') as f:
            f.write('\n'.join(md))
    
    def _generate_readme(self, output_dir: Path):
        """產生 README.md 索引"""
        md = []
        
        md.append("# Event 文件系統\n")
        md.append("本專案的 Event-Driven 架構文件。\n")
        
        md.append("## 📚 快速導航\n")
        md.append("- [完整 Event 目錄](EVENT_CATALOG.md)")
        md.append("- [依 Bounded Context 分類](BOUNDED_CONTEXTS.md)")
        md.append("- [事件流程圖](diagrams/)\n")
        
        md.append("## 📋 Events 列表\n")
        md.append("| Event | Bounded Context | Description | Publishers | Listeners |")
        md.append("|-------|-----------------|-------------|------------|-----------|")
        
        for event_name in sorted(self.events.keys()):
            event = self.events[event_name]
            context = event['bounded_context']
            description = event.get('javadoc', '-')[:50] + '...' if event.get('javadoc') else '-'
            pub_count = len(self.publishers_by_event.get(event_name, []))
            listener_count = len(self.listeners_by_event.get(event_name, []))
            
            md.append(f"| [{event_name}](events/{event_name}.md) | {context} | {description} | {pub_count} | {listener_count} |")
        
        md.append("")
        
        md.append("## 📊 統計資訊\n")
        md.append(f"- **總 Event 數量**: {len(self.events)}")
        md.append(f"- **總 Publisher 數量**: {len(self.publishers_data['publishers'])}")
        md.append(f"- **總 Listener 數量**: {len(self.listeners_data['listeners'])}")
        
        contexts = set(e['bounded_context'] for e in self.events.values())
        md.append(f"- **Bounded Context 數量**: {len(contexts)}\n")
        
        md.append("---")
        md.append(f"*文件產生時間: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}*")
        
        with open(output_dir / 'README.md', 'w', encoding='utf-8') as f:
            f.write('\n'.join(md))
        
        print(f"   ✅ README.md")
    
    def _generate_catalog(self, output_dir: Path):
        """產生 EVENT_CATALOG.md"""
        md = []
        
        md.append("# Event Catalog\n")
        md.append("完整的 Event 目錄,依 Bounded Context 分類。\n")
        
        # 依 Bounded Context 分組
        by_context = {}
        for event_name, event in self.events.items():
            context = event['bounded_context']
            if context not in by_context:
                by_context[context] = []
            by_context[context].append((event_name, event))
        
        for context in sorted(by_context.keys()):
            md.append(f"## {context}\n")
            
            events = by_context[context]
            for event_name, event in sorted(events, key=lambda x: x[0]):
                md.append(f"### [{event_name}](events/{event_name}.md)\n")
                
                if event.get('javadoc'):
                    md.append(event['javadoc'])
                    md.append("")
                
                md.append(f"**Package**: `{event['package']}`\n")
                
                if event['fields']:
                    md.append("**欄位**:")
                    for field in event['fields']:
                        md.append(f"- `{field['name']}`: {field['type']}")
                    md.append("")
        
        with open(output_dir / 'EVENT_CATALOG.md', 'w', encoding='utf-8') as f:
            f.write('\n'.join(md))
        
        print(f"   ✅ EVENT_CATALOG.md")
    
    def _generate_bounded_contexts(self, output_dir: Path):
        """產生 BOUNDED_CONTEXTS.md"""
        md = []
        
        md.append("# Bounded Contexts\n")
        md.append("依 Domain-Driven Design 的 Bounded Context 組織事件。\n")
        
        by_context = {}
        for event_name, event in self.events.items():
            context = event['bounded_context']
            if context not in by_context:
                by_context[context] = {
                    'events': [],
                    'publishers': set(),
                    'listeners': set()
                }
            
            by_context[context]['events'].append(event_name)
            
            for pub in self.publishers_by_event.get(event_name, []):
                by_context[context]['publishers'].add(pub['class_name'])
            
            for listener in self.listeners_by_event.get(event_name, []):
                by_context[context]['listeners'].add(listener['class_name'])
        
        for context in sorted(by_context.keys()):
            data = by_context[context]
            
            md.append(f"## {context}\n")
            md.append(f"**事件數量**: {len(data['events'])}")
            md.append(f"**發送者**: {len(data['publishers'])}")
            md.append(f"**監聽者**: {len(data['listeners'])}\n")
            
            md.append("### Events")
            for event_name in sorted(data['events']):
                md.append(f"- [{event_name}](events/{event_name}.md)")
            md.append("")
        
        with open(output_dir / 'BOUNDED_CONTEXTS.md', 'w', encoding='utf-8') as f:
            f.write('\n'.join(md))
        
        print(f"   ✅ BOUNDED_CONTEXTS.md")


def main():
    parser = argparse.ArgumentParser(description='產生 Event 文件')
    parser.add_argument('--events', type=str, required=True,
                       help='Event 分析結果 JSON')
    parser.add_argument('--publishers', type=str, required=True,
                       help='Publisher 分析結果 JSON')
    parser.add_argument('--listeners', type=str, required=True,
                       help='Listener 分析結果 JSON')
    parser.add_argument('--output-dir', type=str, default='docs/event',
                       help='輸出目錄')
    
    args = parser.parse_args()
    
    generator = EventDocGenerator(
        Path(args.events),
        Path(args.publishers),
        Path(args.listeners)
    )
    
    generator.generate_all(Path(args.output_dir))


if __name__ == '__main__':
    main()
