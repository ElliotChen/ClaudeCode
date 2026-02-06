#!/usr/bin/env python3
"""
文件驗證器 - 檢查產生的 Event 文件是否完整
"""

import sys
import json
import argparse
from pathlib import Path
from typing import List, Dict


class DocumentationValidator:
    """驗證產生的文件"""
    
    def __init__(self, docs_dir: Path, analysis_dir: Path = None):
        self.docs_dir = Path(docs_dir)
        self.analysis_dir = analysis_dir or Path('.event-analysis-temp')
        self.issues: List[str] = []
        self.warnings: List[str] = []
    
    def validate(self) -> bool:
        """執行驗證"""
        print("🔍 驗證 Event 文件...\n")
        
        # 載入分析結果
        if not self._load_analysis_results():
            return False
        
        # 檢查核心檔案
        self._check_core_files()
        
        # 檢查每個 Event 的文件
        self._check_event_files()
        
        # 檢查 Markdown 連結
        self._check_markdown_links()
        
        # 檢查 Mermaid 語法
        self._check_mermaid_syntax()
        
        # 輸出報告
        self._print_report()
        
        return len(self.issues) == 0
    
    def _load_analysis_results(self) -> bool:
        """載入分析結果"""
        try:
            events_file = self.analysis_dir / 'events.json'
            if events_file.exists():
                with open(events_file, 'r', encoding='utf-8') as f:
                    self.events_data = json.load(f)
            else:
                self.issues.append(f"找不到分析結果: {events_file}")
                return False
            
            return True
        except Exception as e:
            self.issues.append(f"載入分析結果時發生錯誤: {e}")
            return False
    
    def _check_core_files(self):
        """檢查核心檔案是否存在"""
        required_files = [
            'README.md',
            'EVENT_CATALOG.md',
            'BOUNDED_CONTEXTS.md'
        ]
        
        for filename in required_files:
            file_path = self.docs_dir / filename
            if not file_path.exists():
                self.issues.append(f"缺少核心檔案: {filename}")
            else:
                # 檢查檔案不為空
                if file_path.stat().st_size == 0:
                    self.issues.append(f"核心檔案為空: {filename}")
    
    def _check_event_files(self):
        """檢查每個 Event 的文件"""
        events_dir = self.docs_dir / 'events'
        
        if not events_dir.exists():
            self.issues.append("events/ 目錄不存在")
            return
        
        # 檢查每個 Event 是否有對應的文件
        for event in self.events_data['events']:
            event_name = event['name']
            event_file = events_dir / f"{event_name}.md"
            
            if not event_file.exists():
                self.issues.append(f"缺少 Event 文件: {event_name}.md")
                continue
            
            # 檢查文件內容
            self._check_event_file_content(event_file, event_name)
    
    def _check_event_file_content(self, file_path: Path, event_name: str):
        """檢查 Event 文件內容"""
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # 必須包含的區段
        required_sections = [
            '## 📋 基本資訊',
            '## 🔧 欄位定義',
            '## 📤 Publisher',
            '## 📥 Listener'
        ]
        
        for section in required_sections:
            if section not in content:
                self.warnings.append(
                    f"{event_name}.md 缺少區段: {section}"
                )
        
        # 檢查是否有 TODO 標記（表示需要人工補充）
        if 'TODO' in content:
            self.warnings.append(
                f"{event_name}.md 包含 TODO 標記,需要人工補充內容"
            )
    
    def _check_markdown_links(self):
        """檢查 Markdown 連結"""
        # 檢查 README.md 中的連結
        readme = self.docs_dir / 'README.md'
        if not readme.exists():
            return
        
        with open(readme, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # 簡單的連結檢查（找出 [text](path) 格式）
        import re
        links = re.findall(r'\[([^\]]+)\]\(([^)]+)\)', content)
        
        for text, link in links:
            # 跳過外部連結
            if link.startswith('http'):
                continue
            
            # 檢查檔案是否存在
            link_path = self.docs_dir / link
            if not link_path.exists():
                self.issues.append(
                    f"README.md 中的連結失效: {link}"
                )
    
    def _check_mermaid_syntax(self):
        """檢查 Mermaid 語法"""
        diagrams_dir = self.docs_dir / 'diagrams'
        
        if not diagrams_dir.exists():
            self.warnings.append("diagrams/ 目錄不存在")
            return
        
        for mmd_file in diagrams_dir.glob('*.mmd'):
            with open(mmd_file, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # 基本語法檢查
            if '```mermaid' not in content:
                self.issues.append(
                    f"{mmd_file.name} 缺少 mermaid 程式碼區塊"
                )
            
            # 檢查是否有基本的圖表類型
            valid_types = ['graph', 'sequenceDiagram', 'classDiagram', 'flowchart']
            if not any(t in content for t in valid_types):
                self.warnings.append(
                    f"{mmd_file.name} 可能缺少有效的圖表類型宣告"
                )
    
    def _print_report(self):
        """輸出驗證報告"""
        print("\n" + "=" * 70)
        print("📊 驗證報告")
        print("=" * 70)
        
        if not self.issues and not self.warnings:
            print("✅ 所有檢查通過！文件完整且正確。\n")
            return
        
        if self.issues:
            print(f"\n❌ 發現 {len(self.issues)} 個問題:\n")
            for i, issue in enumerate(self.issues, 1):
                print(f"   {i}. {issue}")
        
        if self.warnings:
            print(f"\n⚠️  發現 {len(self.warnings)} 個警告:\n")
            for i, warning in enumerate(self.warnings, 1):
                print(f"   {i}. {warning}")
        
        print("\n" + "=" * 70)
        
        if self.issues:
            print("❌ 驗證失敗 - 請修正上述問題")
        else:
            print("✅ 驗證通過 - 但請注意警告事項")
        
        print("=" * 70 + "\n")


def main():
    parser = argparse.ArgumentParser(description='驗證 Event 文件完整性')
    parser.add_argument('--docs-dir', type=str, default='docs/event',
                       help='文件目錄 (預設: docs/event)')
    parser.add_argument('--analysis-dir', type=str,
                       help='分析結果目錄 (預設: .event-analysis-temp)')
    
    args = parser.parse_args()
    
    validator = DocumentationValidator(
        docs_dir=args.docs_dir,
        analysis_dir=Path(args.analysis_dir) if args.analysis_dir else None
    )
    
    success = validator.validate()
    sys.exit(0 if success else 1)


if __name__ == '__main__':
    main()
