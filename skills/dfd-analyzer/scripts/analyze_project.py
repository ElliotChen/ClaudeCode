#!/usr/bin/env python3
"""
Analyze Java/Spring Boot project structure to identify DFD components.
Outputs JSON with detected controllers, services, repositories, and data flows.
"""

import os
import re
import json
import sys
from pathlib import Path
from dataclasses import dataclass, asdict
from typing import List, Dict, Set

@dataclass
class ExternalEntity:
    name: str
    type: str  # 'client', 'external_service', 'database'
    references: List[str]

@dataclass
class Process:
    name: str
    class_name: str
    file_path: str
    type: str  # 'controller', 'service', 'component'
    methods: List[str]
    dependencies: List[str]

@dataclass
class DataStore:
    name: str
    type: str  # 'database', 'cache', 'message_queue'
    entity_class: str
    file_path: str

@dataclass
class DataFlow:
    from_component: str
    to_component: str
    data_type: str
    flow_type: str  # 'request', 'response', 'query', 'event'

class JavaProjectAnalyzer:
    def __init__(self, project_path: str):
        self.project_path = Path(project_path)
        self.external_entities: List[ExternalEntity] = []
        self.processes: List[Process] = []
        self.data_stores: List[DataStore] = []
        self.data_flows: List[DataFlow] = []
        
    def analyze(self) -> Dict:
        """Main analysis method"""
        print(f"Analyzing project: {self.project_path}", file=sys.stderr)
        
        # Find all Java files
        java_files = list(self.project_path.rglob("*.java"))
        print(f"Found {len(java_files)} Java files", file=sys.stderr)
        
        # Analyze each file
        for java_file in java_files:
            self._analyze_file(java_file)
        
        # Infer data flows from dependencies
        self._infer_data_flows()
        
        return {
            'external_entities': [asdict(e) for e in self.external_entities],
            'processes': [asdict(p) for p in self.processes],
            'data_stores': [asdict(d) for d in self.data_stores],
            'data_flows': [asdict(f) for f in self.data_flows]
        }
    
    def _analyze_file(self, file_path: Path):
        """Analyze a single Java file"""
        try:
            content = file_path.read_text(encoding='utf-8')
        except Exception as e:
            print(f"Error reading {file_path}: {e}", file=sys.stderr)
            return
        
        # Extract class name
        class_match = re.search(r'public\s+(?:class|interface)\s+(\w+)', content)
        if not class_match:
            return
        
        class_name = class_match.group(1)
        relative_path = str(file_path.relative_to(self.project_path))
        
        # Check for Spring annotations to determine component type
        if re.search(r'@RestController|@Controller', content):
            self._analyze_controller(class_name, content, relative_path)
        elif re.search(r'@Service', content):
            self._analyze_service(class_name, content, relative_path)
        elif re.search(r'@Repository', content):
            self._analyze_repository(class_name, content, relative_path)
        elif re.search(r'@Entity|@Table', content):
            self._analyze_entity(class_name, content, relative_path)
        elif re.search(r'@FeignClient|@RestTemplate', content):
            self._analyze_external_client(class_name, content, relative_path)
    
    def _analyze_controller(self, class_name: str, content: str, file_path: str):
        """Analyze REST controller"""
        methods = re.findall(r'@(?:Get|Post|Put|Delete|Patch)Mapping.*?\n.*?public\s+\w+\s+(\w+)\s*\(', content, re.DOTALL)
        
        # Extract dependencies (Autowired services)
        dependencies = re.findall(r'@Autowired.*?\n.*?private\s+(\w+)', content, re.DOTALL)
        
        process = Process(
            name=class_name,
            class_name=class_name,
            file_path=file_path,
            type='controller',
            methods=methods,
            dependencies=dependencies
        )
        self.processes.append(process)
        
        # Add external entity for API clients
        if class_name not in [e.name for e in self.external_entities]:
            self.external_entities.append(ExternalEntity(
                name=f"API Client ({class_name})",
                type='client',
                references=[class_name]
            ))
    
    def _analyze_service(self, class_name: str, content: str, file_path: str):
        """Analyze service layer"""
        methods = re.findall(r'public\s+\w+\s+(\w+)\s*\(', content)
        dependencies = re.findall(r'@Autowired.*?\n.*?private\s+(\w+)', content, re.DOTALL)
        
        process = Process(
            name=class_name,
            class_name=class_name,
            file_path=file_path,
            type='service',
            methods=methods[:10],  # Limit to first 10 methods
            dependencies=dependencies
        )
        self.processes.append(process)
    
    def _analyze_repository(self, class_name: str, content: str, file_path: str):
        """Analyze repository/DAO"""
        methods = re.findall(r'(?:find|save|delete|update)\w+', content)
        
        process = Process(
            name=class_name,
            class_name=class_name,
            file_path=file_path,
            type='repository',
            methods=list(set(methods)),
            dependencies=[]
        )
        self.processes.append(process)
    
    def _analyze_entity(self, class_name: str, content: str, file_path: str):
        """Analyze JPA entity for data store identification"""
        table_match = re.search(r'@Table\(name\s*=\s*"(\w+)"', content)
        table_name = table_match.group(1) if table_match else class_name.lower()
        
        data_store = DataStore(
            name=table_name,
            type='database',
            entity_class=class_name,
            file_path=file_path
        )
        self.data_stores.append(data_store)
    
    def _analyze_external_client(self, class_name: str, content: str, file_path: str):
        """Analyze Feign clients or external service calls"""
        # Extract URL from @FeignClient
        url_match = re.search(r'@FeignClient\(.*?url\s*=\s*"([^"]+)"', content)
        service_name = re.search(r'@FeignClient\(.*?name\s*=\s*"([^"]+)"', content)
        
        name = service_name.group(1) if service_name else url_match.group(1) if url_match else class_name
        
        external_entity = ExternalEntity(
            name=name,
            type='external_service',
            references=[class_name]
        )
        self.external_entities.append(external_entity)
    
    def _infer_data_flows(self):
        """Infer data flows from component dependencies"""
        process_map = {p.class_name: p for p in self.processes}
        
        for process in self.processes:
            for dep in process.dependencies:
                if dep in process_map:
                    target = process_map[dep]
                    
                    # Determine flow type based on component types
                    flow_type = 'request'
                    if process.type == 'controller' and target.type == 'service':
                        flow_type = 'request'
                    elif process.type == 'service' and target.type == 'repository':
                        flow_type = 'query'
                    
                    data_flow = DataFlow(
                        from_component=process.class_name,
                        to_component=target.class_name,
                        data_type='business_data',
                        flow_type=flow_type
                    )
                    self.data_flows.append(data_flow)

def main():
    if len(sys.argv) < 2:
        print("Usage: analyze_project.py <project_path>", file=sys.stderr)
        sys.exit(1)
    
    project_path = sys.argv[1]
    
    if not os.path.exists(project_path):
        print(f"Error: Project path does not exist: {project_path}", file=sys.stderr)
        sys.exit(1)
    
    analyzer = JavaProjectAnalyzer(project_path)
    result = analyzer.analyze()
    
    # Output JSON to stdout
    print(json.dumps(result, indent=2))

if __name__ == '__main__':
    main()
