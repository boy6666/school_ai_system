// 字符串 Java 实操案例
// 说明：这是示例代码，供练习参考。

class TreeNode {
    int val;
    TreeNode left;
    TreeNode right;

    TreeNode(int val) {
        this.val = val;
    }
}

public class Practice {
    public static int height(TreeNode root) {
        // 边界条件：空树高度为 0
        if (root == null) {
            return 0;
        }

        int leftHeight = height(root.left);
        int rightHeight = height(root.right);

        return Math.max(leftHeight, rightHeight) + 1;
    }
}