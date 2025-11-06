package org.example.Mermaid

enum class MermaidGraphType(val header: String) {
    FLOWCHART_TD("flowchart TD"),
    FLOWCHART_LR("flowchart LR"),
    FLOWCHART_BT("flowchart BT"),
    GRAPH_TD("graph TD"),
    GRAPH_LR("graph LR"),
    SEQUENCE("sequenceDiagram"),
    CLASS("classDiagram")
}