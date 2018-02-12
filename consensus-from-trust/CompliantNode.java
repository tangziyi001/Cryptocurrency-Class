import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.HashMap;
import java.lang.Math;

/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {
    HashSet<Transaction> allTx;
    boolean[] followees;
    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
        // IMPLEMENT THIS
        allTx = new HashSet<Transaction>();
    }

    public void setFollowees(boolean[] followees) {
        // IMPLEMENT THIS
        this.followees = Arrays.copyOf(followees, followees.length);
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        // IMPLEMENT THIS
        for (Transaction tx: pendingTransactions) {
            allTx.add(tx);
        }
    }

    public Set<Transaction> sendToFollowers() {
        // IMPLEMENT THIS
        return allTx;
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
        HashSet<Integer> activeSenders = new HashSet<Integer>();
        for (Candidate c : candidates) {
            activeSenders.add(c.sender);
        }
        for (int i = 0; i < followees.length; i++) {
            if (!activeSenders.contains(i)) {
                this.followees[i] = false;
            }
        }
        for (Candidate c : candidates) {
            if (followees[c.sender])
                allTx.add(new Transaction(c.tx.id));
        }
    }
}
