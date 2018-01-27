import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.lang.Math;

/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {
    Set<Transaction> allTx;
    int numParents;
    double p_malicious;
    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
        // IMPLEMENT THIS
        allTx = new HashSet<Transaction>();
        this.p_malicious = p_malicious;
    }

    public void setFollowees(boolean[] followees) {
        // IMPLEMENT THIS
        this.numParents = followees.length;
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
        // IMPLEMENT THIS
        for (Candidate c : candidates) {
            allTx.add(new Transaction(c.tx.id));
        }
    }
}
