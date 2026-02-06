#!/usr/bin/env python3
"""
Generate Mermaid DFD diagram from analysis results.
Takes JSON analysis output and produces Mermaid flowchart syntax.
"""

import json
import sys
from typing import Dict, List

class MermaidDFDGenerator:
    def __init__(self, analysis_data: Dict):
        self.data = analysis_data
        self.node_ids = {}
        self.counter = 0
        
    def _get_node_id(self, name: str) -> str:
        """Generate or retrieve node ID for a component"""
        if name not in self.node_ids:
            self.counter += 1
            self.node_ids[name] = f"node{self.counter}"
        return self.node_ids[name]
    
    def _get_shape(self, component_type: str) -> tuple:
        """Return Mermaid shape syntax for different component types"""
        shapes = {
            'client': ('[[', ']]'),  # External entity - double brackets
            'external_service': ('[[', ']]'),
            'database': ('[[(', ')]]'),  # Data store - cylinder
            'controller': ('[', ']'),  # Process - rectangle
            'service': ('[', ']'),
            'repository': ('[', ']'),
            'cache': ('[[(', ')]]'),
            'message_queue': ('[[(', ')]]'),
        }
        return shapes.get(component_type, ('[', ']'))
    
    def generate(self) -> str:
        """Generate complete Mermaid DFD"""
        lines = ['graph TB']
        lines.append('    %% External Entities')
        
        # Add external entities
        for entity in self.data.get('external_entities', []):
            node_id = self._get_node_id(entity['name'])
            start_shape, end_shape = self._get_shape(entity['type'])
            lines.append(f"    {node_id}{start_shape}\"{entity['name']}<br/>({entity['type']})\"{end_shape}")
        
        lines.append('')
        lines.append('    %% Processes')
        
        # Add processes
        for process in self.data.get('processes', []):
            node_id = self._get_node_id(process['class_name'])
            start_shape, end_shape = self._get_shape(process['type'])
            
            # Include method count in label
            method_info = f"<br/>{len(process['methods'])} methods" if process['methods'] else ""
            lines.append(f"    {node_id}{start_shape}\"{process['name']}<br/>({process['type']}){method_info}\"{end_shape}")
        
        lines.append('')
        lines.append('    %% Data Stores')
        
        # Add data stores
        for store in self.data.get('data_stores', []):
            node_id = self._get_node_id(store['name'])
            start_shape, end_shape = self._get_shape(store['type'])
            lines.append(f"    {node_id}{start_shape}\"{store['name']}<br/>({store['entity_class']})\"{end_shape}")
        
        lines.append('')
        lines.append('    %% Data Flows')
        
        # Add data flows
        for flow in self.data.get('data_flows', []):
            from_id = self._get_node_id(flow['from_component'])
            to_id = self._get_node_id(flow['to_component'])
            label = flow['flow_type']
            lines.append(f"    {from_id} -->|{label}| {to_id}")
        
        # Add connections from external entities to controllers
        for entity in self.data.get('external_entities', []):
            if entity['type'] == 'client':
                for ref in entity['references']:
                    if ref in self.node_ids:
                        entity_id = self._get_node_id(entity['name'])
                        ref_id = self._get_node_id(ref)
                        lines.append(f"    {entity_id} -->|HTTP Request| {ref_id}")
        
        # Add connections from repositories to data stores
        for process in self.data.get('processes', []):
            if process['type'] == 'repository':
                # Try to match repository with entity
                for store in self.data.get('data_stores', []):
                    # Simple heuristic: if repository name contains entity name
                    if store['entity_class'].lower() in process['class_name'].lower():
                        repo_id = self._get_node_id(process['class_name'])
                        store_id = self._get_node_id(store['name'])
                        lines.append(f"    {repo_id} <-->|CRUD| {store_id}")
        
        lines.append('')
        lines.append('    %% Styling')
        lines.append('    classDef externalEntity fill:#e1f5ff,stroke:#01579b,stroke-width:2px')
        lines.append('    classDef process fill:#fff9c4,stroke:#f57f17,stroke-width:2px')
        lines.append('    classDef dataStore fill:#f3e5f5,stroke:#4a148c,stroke-width:2px')
        
        # Apply styles
        for entity in self.data.get('external_entities', []):
            node_id = self._get_node_id(entity['name'])
            lines.append(f"    class {node_id} externalEntity")
        
        for process in self.data.get('processes', []):
            node_id = self._get_node_id(process['class_name'])
            lines.append(f"    class {node_id} process")
        
        for store in self.data.get('data_stores', []):
            node_id = self._get_node_id(store['name'])
            lines.append(f"    class {node_id} dataStore")
        
        return '\n'.join(lines)
    
    def generate_context_diagram(self) -> str:
        """Generate Level 0 context diagram (system boundary view)"""
        lines = ['graph TB']
        lines.append('    %% Context Diagram - Level 0')
        lines.append('')
        
        # Create a single system node
        system_id = 'system'
        lines.append(f'    {system_id}["System<br/>(Application)"]')
        
        # Add external entities
        external_nodes = []
        for entity in self.data.get('external_entities', []):
            node_id = self._get_node_id(entity['name'])
            external_nodes.append(node_id)
            start_shape, end_shape = self._get_shape(entity['type'])
            lines.append(f"    {node_id}{start_shape}\"{entity['name']}\"{end_shape}")
            
            # Connect to system
            if entity['type'] == 'client':
                lines.append(f"    {node_id} -->|Requests| {system_id}")
                lines.append(f"    {system_id} -->|Responses| {node_id}")
            elif entity['type'] == 'external_service':
                lines.append(f"    {system_id} -->|API Calls| {node_id}")
        
        # Add data stores
        for store in self.data.get('data_stores', []):
            node_id = self._get_node_id(store['name'])
            start_shape, end_shape = self._get_shape(store['type'])
            lines.append(f"    {node_id}{start_shape}\"{store['name']}\"{end_shape}")
            lines.append(f"    {system_id} <-->|Data| {node_id}")
        
        lines.append('')
        lines.append('    %% Styling')
        lines.append('    classDef systemNode fill:#ffecb3,stroke:#ff6f00,stroke-width:3px')
        lines.append('    classDef externalEntity fill:#e1f5ff,stroke:#01579b,stroke-width:2px')
        lines.append('    classDef dataStore fill:#f3e5f5,stroke:#4a148c,stroke-width:2px')
        lines.append(f'    class {system_id} systemNode')
        
        for entity in self.data.get('external_entities', []):
            node_id = self._get_node_id(entity['name'])
            lines.append(f"    class {node_id} externalEntity")
        
        for store in self.data.get('data_stores', []):
            node_id = self._get_node_id(store['name'])
            lines.append(f"    class {node_id} dataStore")
        
        return '\n'.join(lines)

def main():
    if len(sys.argv) < 2:
        print("Usage: generate_mermaid.py <analysis.json> [--context]", file=sys.stderr)
        print("  --context: Generate context diagram (Level 0) instead of detailed DFD", file=sys.stderr)
        sys.exit(1)
    
    analysis_file = sys.argv[1]
    is_context = '--context' in sys.argv
    
    try:
        with open(analysis_file, 'r') as f:
            analysis_data = json.load(f)
    except Exception as e:
        print(f"Error reading analysis file: {e}", file=sys.stderr)
        sys.exit(1)
    
    generator = MermaidDFDGenerator(analysis_data)
    
    if is_context:
        diagram = generator.generate_context_diagram()
    else:
        diagram = generator.generate()
    
    print(diagram)

if __name__ == '__main__':
    main()
