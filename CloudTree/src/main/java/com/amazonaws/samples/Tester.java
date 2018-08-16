package com.amazonaws.samples;

public class Tester {
	public static void main(String[]args) {
		CloudTree<String> tree = new CloudTree<>("Cloud_Tree_Tester_Strings");
		CloudTree<Integer> tree2 = new CloudTree<>("Cloud_Tree_Tester_Integers");
		
		int[]input = {5,2,7,9,1,4,6,11,8};
		String[]input2 = {"Bob","Alice","Jim","Tom","jamo","Debby","Karl","Linaeus","Mark"};
		
		for(int i=0;i<input2.length;i++) {
			tree.insert(input2[i]);
			tree2.insert(input[i]);
			try {
				Thread.sleep(1000);
			}catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println(tree.getDetails());
		
		System.out.println("query is there a 3 "+tree2.query(3));
		System.out.println("query is there Alice "+tree.query("Alice"));
		
		System.out.println("\nInteger tree");
		System.out.println("in order traversal "+tree2.printInOrder());
		System.out.println("Post order traversal "+tree2.printPostOrder());
		System.out.println("Pre order traversal "+tree2.printPreOrder());
		System.out.println("the local tree contains "+tree2.printLocalTree());
		System.out.println("\nString tree");
		
		System.out.println("in order traversal "+tree.printInOrder());
		System.out.println("Post order traversal "+tree.printPostOrder());
		System.out.println("Pre order traversal "+tree.printPreOrder());
		System.out.println("the local tree contains "+tree.printLocalTree());
	}
}
