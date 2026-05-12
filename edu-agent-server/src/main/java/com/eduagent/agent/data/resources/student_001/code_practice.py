class Node:
    def __init__(self, val):
        self.val = val
        self.left = None
        self.right = None

def preorder(root):
    if root is None:
        return []
    return [root.val] + preorder(root.left) + preorder(root.right)
