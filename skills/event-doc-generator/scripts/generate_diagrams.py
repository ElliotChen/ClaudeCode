#!/usr/bin/env python3
"""
Mermaid 流程圖產生器 - 產生事件架構的視覺化圖表
"""

import json
import argparse
from pathlib import Path
from typing import Dict, List, Set
from collections import defaultdict


class MermaidDiagramGenerator:
    """Mermaid 流程圖產生器"""
    
    def __init__(self, events_file: Path, publishers_file: Path, listeners_file: Path):
        # 載入分析結果
        with open(events_file, 'r', encoding='utf-8') as f:
            self.events_data = json.load(f)
        
        with open(publishers_file, 'r', encoding='utf-8') as f:
            self.publishers_data = json.load(f)
        
        with open(listeners_file, 'r', encoding='utf-8') as f:
            self.listeners_data = json.load(f)
    
    def generate_all(self, output_dir: Path):
        """產生所有圖表"""
        output_dir.mkdir(parents=True, exist_ok=True)
        
        print("📊 產生 Mermaid 流程圖...")
        
        # 1. 整體事件關聯圖
        self._generate_event_graph(output_dir / 'event-overview.mmd')
        
        # 2. 依 Bounded Context 的圖表
        self._generate_context_diagrams(output_dir)
        
        # 3. 事件序列圖
        self._generate_sequence_diagrams(output_dir / 'sequence-diagrams.mmd')
        
        print(f"✅ 流程圖已產生至: {output_dir}")
    
    def _generate_event_graph(self, output_path: Path):
        """產生整體事件關聯圖"""
        lines = []
        lines.append("```mermaid")
        lines.append("graph TD")
        lines.append("    %% Event-Driven Architecture Overview")
        lines.append("")
        
        # 依 Bounded Context 分組
        by_context = defaultdict(list)
        for event in self.events_data['events']:
            by_context[event['bounded_context']].append(event['name'])
        
        # 為每個 Context 建立子圖
        for context, events in sorted(by_context.items()):
            context_id = context.replace(' ', '_')
            lines.append(f"    subgraph {context_id}[{context}]")
            for event_name in events:
                lines.append(f"        {event_name}[({event_name})]")
            lines.append("    end")
            lines.append("")
        
        # 建立 Publisher -> Event 連線
        publisher_to_events = defaultdict(set)
        for pub in self.publishers_data['publishers']:
            class_name = pub['class_name']
            event_name = pub['event_name']
            publisher_to_events[class_name].add(event_name)
        
        if publisher_to_events:
            lines.append("    %% Publishers")
            for publisher, events in sorted(publisher_to_events.items()):
                pub_id = publisher.replace(' ', '_')
                lines.append(f"    {pub_id}[{publisher}]")
                for event in events:
                    lines.append(f"    {pub_id} -->|publish| {event}")
            lines.append("")
        
        # 建立 Event -> Listener 連線
        event_to_listeners = defaultdict(set)
        for listener in self.listeners_data['listeners']:
            class_name = listener['class_name']
            event_name = listener['event_name']
            event_to_listeners[event_name].add(class_name)
        
        if event_to_listeners:
            lines.append("    %% Listeners")
            for event, listeners in sorted(event_to_listeners.items()):
                for listener in listeners:
                    listener_id = listener.replace(' ', '_')
                    
                    # 找出這個 listener 的詳細資訊
                    listener_info = next(
                        (l for l in self.listeners_data['listeners'] 
                         if l['class_name'] == listener and l['event_name'] == event),
                        None
                    )
                    
                    # 根據 listener 類型使用不同的樣式
                    if listener_info:
                        if listener_info.get('is_async'):
                            lines.append(f"    {listener_id}{{{{async: {listener}}}}}")
                            lines.append(f"    {event} -.->|async| {listener_id}")
                        elif listener_info.get('is_transactional'):
                            phase = listener_info.get('transaction_phase', 'TX')
                            lines.append(f"    {listener_id}[/{listener}/]")
                            lines.append(f"    {event} -->|{phase}| {listener_id}")
                        else:
                            lines.append(f"    {listener_id}[{listener}]")
                            lines.append(f"    {event} --> {listener_id}")
                    else:
                        lines.append(f"    {listener_id}[{listener}]")
                        lines.append(f"    {event} --> {listener_id}")
            lines.append("")
        
        # 樣式定義
        lines.append("    %% Styling")
        lines.append("    classDef eventClass fill:#e1f5ff,stroke:#01579b,stroke-width:2px")
        lines.append("    classDef publisherClass fill:#fff3e0,stroke:#e65100,stroke-width:2px")
        lines.append("    classDef listenerClass fill:#f3e5f5,stroke:#4a148c,stroke-width:2px")
        lines.append("")
        
        for event in self.events_data['events']:
            lines.append(f"    class {event['name']} eventClass")
        
        lines.append("```")
        
        with open(output_path, 'w', encoding='utf-8') as f:
            f.write('\n'.join(lines))
        
        print(f"   ✅ event-overview.mmd")
    
    def _generate_context_diagrams(self, output_dir: Path):
        """為每個 Bounded Context 產生獨立圖表"""
        by_context = defaultdict(lambda: {'events': [], 'publishers': set(), 'listeners': set()})
        
        # 收集每個 Context 的資訊
        for event in self.events_data['events']:
            context = event['bounded_context']
            event_name = event['name']
            by_context[context]['events'].append(event_name)
        
        for pub in self.publishers_data['publishers']:
            event_name = pub['event_name']
            # 找出這個 event 的 context
            event = next((e for e in self.events_data['events'] if e['name'] == event_name), None)
            if event:
                context = event['bounded_context']
                by_context[context]['publishers'].add((pub['class_name'], event_name))
        
        for listener in self.listeners_data['listeners']:
            event_name = listener['event_name']
            event = next((e for e in self.events_data['events'] if e['name'] == event_name), None)
            if event:
                context = event['bounded_context']
                by_context[context]['listeners'].add((listener['class_name'], event_name))
        
        # 為每個 Context 產生圖表
        for context, data in by_context.items():
            if not data['events']:
                continue
            
            filename = f"{context.lower().replace(' ', '-')}-context.mmd"
            self._generate_single_context_diagram(
                output_dir / filename,
                context,
                data
            )
            print(f"   ✅ {filename}")
    
    def _generate_single_context_diagram(self, output_path: Path, context: str, data: Dict):
        """產生單一 Context 的圖表"""
        lines = []
        lines.append("```mermaid")
        lines.append("graph LR")
        lines.append(f"    %% {context} Bounded Context")
        lines.append("")
        
        # Events
        lines.append("    %% Events")
        for event in data['events']:
            lines.append(f"    {event}[({event})]")
        lines.append("")
        
        # Publishers
        if data['publishers']:
            lines.append("    %% Publishers")
            for publisher, event in data['publishers']:
                pub_id = publisher.replace(' ', '_')
                lines.append(f"    {pub_id}[{publisher}]")
                lines.append(f"    {pub_id} --> {event}")
            lines.append("")
        
        # Listeners
        if data['listeners']:
            lines.append("    %% Listeners")
            for listener, event in data['listeners']:
                listener_id = listener.replace(' ', '_')
                lines.append(f"    {listener_id}[{listener}]")
                lines.append(f"    {event} --> {listener_id}")
            lines.append("")
        
        lines.append("```")
        
        with open(output_path, 'w', encoding='utf-8') as f:
            f.write('\n'.join(lines))
    
    def _generate_sequence_diagrams(self, output_path: Path):
        """產生序列圖"""
        lines = []
        lines.append("# Event Sequence Diagrams\n")
        lines.append("事件的序列圖展示。\n")
        
        # 為每個 Event 產生序列圖
        for event in self.events_data['events'][:10]:  # 限制數量避免太長
            event_name = event['name']
            
            publishers = [p for p in self.publishers_data['publishers'] if p['event_name'] == event_name]
            listeners = [l for l in self.listeners_data['listeners'] if l['event_name'] == event_name]
            
            if not publishers and not listeners:
                continue
            
            lines.append(f"## {event_name}\n")
            lines.append("```mermaid")
            lines.append("sequenceDiagram")
            
            # 參與者
            for pub in publishers[:3]:
                lines.append(f"    participant {pub['class_name']}")
            
            lines.append(f"    participant {event_name}")
            
            for listener in listeners[:5]:
                lines.append(f"    participant {listener['class_name']}")
            
            lines.append("")
            
            # 互動
            for pub in publishers[:3]:
                lines.append(f"    {pub['class_name']}->>+{event_name}: publish")
            
            for listener in listeners[:5]:
                annotation = ""
                if listener.get('is_async'):
                    annotation = " (async)"
                elif listener.get('is_transactional'):
                    phase = listener.get('transaction_phase', 'AFTER_COMMIT')
                    annotation = f" ({phase})"
                
                lines.append(f"    {event_name}->>+{listener['class_name']}: handle{annotation}")
                lines.append(f"    {listener['class_name']}-->>-{event_name}: done")
            
            if publishers:
                lines.append(f"    {event_name}-->>-{publishers[0]['class_name']}: completed")
            
            lines.append("```\n")
        
        with open(output_path, 'w', encoding='utf-8') as f:
            f.write('\n'.join(lines))
        
        print(f"   ✅ sequence-diagrams.mmd")


def main():
    parser = argparse.ArgumentParser(description='產生 Mermaid 流程圖')
    parser.add_argument('--events', type=str, required=True,
                       help='Event 分析結果 JSON')
    parser.add_argument('--publishers', type=str, required=True,
                       help='Publisher 分析結果 JSON')
    parser.add_argument('--listeners', type=str, required=True,
                       help='Listener 分析結果 JSON')
    parser.add_argument('--output-dir', type=str, required=True,
                       help='輸出目錄')
    
    args = parser.parse_args()
    
    generator = MermaidDiagramGenerator(
        Path(args.events),
        Path(args.publishers),
        Path(args.listeners)
    )
    
    generator.generate_all(Path(args.output_dir))


if __name__ == '__main__':
    main()
