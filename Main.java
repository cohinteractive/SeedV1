public class Main {
    
    public static void main(String[] args) {
        Eval eval = new Eval(1);
        long[] parent = Board.fromFen("4k3/8/8/8/8/8/8/4K3 w - - 0 1");
        long[] child = Board.fromFen("4k3/8/8/8/8/8/8/3K4 b - - 0 1");
        eval.root(parent[0], parent[1], parent[2], parent[3]);
        eval.child(parent[0], parent[1], parent[2], parent[3], child[0], child[1], child[2], child[3], 0);
        int incremental = eval.evaluate(child[0], child[1], child[2], child[3], 1);
        int rebuilt = eval.evaluate(child[0], child[1], child[2], child[3], (int) child[4]);
        if(incremental != rebuilt)
            throw new AssertionError("Incremental evaluation mismatch");
        System.out.println("Evaluation parity ok: " + incremental);
    }

    private Main() {}

}
