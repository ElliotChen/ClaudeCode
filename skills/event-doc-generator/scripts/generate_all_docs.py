#!/usr/bin/env python3
"""
Event 文件產生器 - 主程式
整合所有分析步驟並產生完整的事件文件
"""

import sys
import argparse
import subprocess
from pathlib import Path
from datetime import datetime


class EventDocPipeline:
    """Event 文件產生管線"""
    
    def __init__(self, source_dir: Path, output_dir: Path, temp_dir: Path = None):
        self.source_dir = Path(source_dir)
        self.output_dir = Path(output_dir)
        self.temp_dir = temp_dir or Path.cwd() / '.event-analysis-temp'
        
        # 確保目錄存在
        self.temp_dir.mkdir(parents=True, exist_ok=True)
        self.output_dir.mkdir(parents=True, exist_ok=True)
        
        # 分析結果檔案路徑
        self.events_json = self.temp_dir / 'events.json'
        self.publishers_json = self.temp_dir / 'publishers.json'
        self.listeners_json = self.temp_dir / 'listeners.json'
        
        # 取得腳本目錄
        self.script_dir = Path(__file__).parent
    
    def run(self):
        """執行完整的文件產生流程"""
        print("=" * 70)
        print("🚀 Event Documentation Generator")
        print("=" * 70)
        print(f"📂 原始碼目錄: {self.source_dir}")
        print(f"📁 輸出目錄: {self.output_dir}")
        print(f"🕐 開始時間: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print("=" * 70)
        print()
        
        try:
            # 步驟 1: 掃描 Event
            self._step1_scan_events()
            
            # 步驟 2: 分析 Publisher
            self._step2_analyze_publishers()
            
            # 步驟 3: 分析 Listener
            self._step3_analyze_listeners()
            
            # 步驟 4: 產生文件
            self._step4_generate_docs()
            
            # 步驟 5: 產生流程圖
            self._step5_generate_diagrams()
            
            print()
            print("=" * 70)
            print("✨ 文件產生完成!")
            print(f"📁 請查看: {self.output_dir}")
            print("=" * 70)
            
            return 0
            
        except Exception as e:
            print()
            print("=" * 70)
            print(f"❌ 發生錯誤: {e}")
            print("=" * 70)
            return 1
    
    def _step1_scan_events(self):
        """步驟 1: 掃描 Event 類別"""
        print("📝 步驟 1/5: 掃描 Event 類別")
        print("-" * 70)
        
        cmd = [
            sys.executable,
            str(self.script_dir / 'scan_events.py'),
            '--source-dir', str(self.source_dir),
            '--output', str(self.events_json)
        ]
        
        result = subprocess.run(cmd, capture_output=False)
        if result.returncode != 0:
            raise Exception("Event 掃描失敗")
        
        print()
    
    def _step2_analyze_publishers(self):
        """步驟 2: 分析 Publisher"""
        print("📝 步驟 2/5: 分析 Event Publisher")
        print("-" * 70)
        
        cmd = [
            sys.executable,
            str(self.script_dir / 'analyze_publishers.py'),
            '--source-dir', str(self.source_dir),
            '--output', str(self.publishers_json)
        ]
        
        result = subprocess.run(cmd, capture_output=False)
        if result.returncode != 0:
            raise Exception("Publisher 分析失敗")
        
        print()
    
    def _step3_analyze_listeners(self):
        """步驟 3: 分析 Listener"""
        print("📝 步驟 3/5: 分析 Event Listener")
        print("-" * 70)
        
        cmd = [
            sys.executable,
            str(self.script_dir / 'analyze_listeners.py'),
            '--source-dir', str(self.source_dir),
            '--output', str(self.listeners_json)
        ]
        
        result = subprocess.run(cmd, capture_output=False)
        if result.returncode != 0:
            raise Exception("Listener 分析失敗")
        
        print()
    
    def _step4_generate_docs(self):
        """步驟 4: 產生文件"""
        print("📝 步驟 4/5: 產生 Markdown 文件")
        print("-" * 70)
        
        cmd = [
            sys.executable,
            str(self.script_dir / 'generate_event_docs.py'),
            '--events', str(self.events_json),
            '--publishers', str(self.publishers_json),
            '--listeners', str(self.listeners_json),
            '--output-dir', str(self.output_dir)
        ]
        
        result = subprocess.run(cmd, capture_output=False)
        if result.returncode != 0:
            raise Exception("文件產生失敗")
        
        print()
    
    def _step5_generate_diagrams(self):
        """步驟 5: 產生流程圖"""
        print("📝 步驟 5/5: 產生 Mermaid 流程圖")
        print("-" * 70)
        
        # 檢查是否有 diagram generator
        diagram_script = self.script_dir / 'generate_diagrams.py'
        if diagram_script.exists():
            cmd = [
                sys.executable,
                str(diagram_script),
                '--events', str(self.events_json),
                '--publishers', str(self.publishers_json),
                '--listeners', str(self.listeners_json),
                '--output-dir', str(self.output_dir / 'diagrams')
            ]
            
            result = subprocess.run(cmd, capture_output=False)
            if result.returncode != 0:
                print("⚠️  流程圖產生失敗,但不影響主要文件")
        else:
            print("⚠️  找不到 generate_diagrams.py,跳過流程圖產生")
        
        print()
    
    def clean_temp(self):
        """清理暫存檔案"""
        import shutil
        if self.temp_dir.exists():
            shutil.rmtree(self.temp_dir)
            print(f"🗑️  已清理暫存目錄: {self.temp_dir}")


def main():
    parser = argparse.ArgumentParser(
        description='Spring Boot Event Documentation Generator',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
範例:
  # 基本使用
  python generate_all_docs.py --source-dir src/main/java --output-dir docs/event
  
  # 指定暫存目錄
  python generate_all_docs.py --source-dir src/main/java --output-dir docs/event --temp-dir /tmp/event-analysis
  
  # 執行後清理暫存檔
  python generate_all_docs.py --source-dir src/main/java --output-dir docs/event --clean
        """
    )
    
    parser.add_argument('--source-dir', type=str, required=True,
                       help='Java 原始碼目錄 (例如: src/main/java)')
    parser.add_argument('--output-dir', type=str, default='docs/event',
                       help='文件輸出目錄 (預設: docs/event)')
    parser.add_argument('--temp-dir', type=str,
                       help='暫存目錄 (預設: .event-analysis-temp)')
    parser.add_argument('--clean', action='store_true',
                       help='執行後清理暫存檔案')
    
    args = parser.parse_args()
    
    # 建立管線
    pipeline = EventDocPipeline(
        source_dir=args.source_dir,
        output_dir=args.output_dir,
        temp_dir=Path(args.temp_dir) if args.temp_dir else None
    )
    
    # 執行
    exit_code = pipeline.run()
    
    # 清理
    if args.clean:
        pipeline.clean_temp()
    
    sys.exit(exit_code)


if __name__ == '__main__':
    main()
