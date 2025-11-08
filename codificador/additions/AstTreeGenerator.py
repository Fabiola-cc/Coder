import re
import sys
from graphviz import Digraph

class Node:
    def __init__(self, name):
        self.name = name
        self.children = []

def parse_sexpr(text: str) -> Node:
    # Limpieza: eliminar caracteres extraños como <EOF> o espacios dobles
    text = text.replace("<EOF>", "").strip()

    # Tokenización más segura (mantiene paréntesis y nombres con símbolos)
    tokens = re.findall(r'\(|\)|[^\s()]+', text)
    index = 0

    def parse_node():
        nonlocal index
        if index >= len(tokens):
            return None  # evita index out of range

        token = tokens[index]
        if token == "(":
            index += 1
            if index >= len(tokens):
                return None
            name = tokens[index]
            index += 1
            node = Node(name)
            while index < len(tokens) and tokens[index] != ")":
                child = parse_node()
                if child:
                    node.children.append(child)
            if index < len(tokens):
                index += 1  # consume ')'
            return node
        elif token == ")":
            # Paréntesis sin nodo — ignóralo
            index += 1
            return None
        else:
            index += 1
            return Node(token)

    root = parse_node()
    return root

def build_graphviz(node, dot=None, parent_id=None, counter=[0]):
    if node is None:
        return dot

    if dot is None:
        dot = Digraph(comment="AST", format="png")
        dot.attr(rankdir="TB", splines="ortho", nodesep="0.4", ranksep="0.8")

    node_id = f"n{counter[0]}"
    counter[0] += 1
    dot.node(node_id, node.name)

    if parent_id:
        dot.edge(parent_id, node_id)

    for child in node.children:
        build_graphviz(child, dot, node_id, counter)

    return dot

if __name__ == "__main__":
    text = sys.stdin.read().strip()

    if not text:
        print("[ERROR] No input received from Java", file=sys.stderr)
        sys.exit(1)

    try:
        root = parse_sexpr(text)
        if not root:
            raise ValueError("Empty or invalid parse tree")
        dot = build_graphviz(root)
        output_path = dot.render("ast_tree", cleanup=True)
        print(output_path)
    except Exception as e:
        print(f"[ERROR] {e}", file=sys.stderr)
        sys.exit(1)
