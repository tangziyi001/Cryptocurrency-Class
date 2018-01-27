import java.util.HashSet;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;

public class TxHandler {

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */

    public UTXOPool utxoPool;
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * Print Transaction in Handler
     */
    public void printTransaction(Transaction tx) {
        ArrayList<Transaction.Input> inputs = tx.getInputs();
        ArrayList<Transaction.Output> outputs = tx.getOutputs();
        // Check all inputs
        System.out.printf("\nCurrent Hash: %s\n", Arrays.toString(tx.getHash()));
        for (int i = 0; i < inputs.size(); i++) {
            System.out.printf("--- Input %d\n", i);
            Transaction.Input nowInput = inputs.get(i);
            System.out.printf("Prev Hash: %s\n", Arrays.toString(nowInput.prevTxHash));
            System.out.printf("Prev Output Idx %s\n", nowInput.outputIndex);
            UTXO utxo = new UTXO(nowInput.prevTxHash, nowInput.outputIndex);
            Transaction.Output prevOutput = utxoPool.getTxOutput(utxo);
            System.out.printf("Prev Output Val: %f\n", prevOutput.value);
        }
        for (int i = 0; i < outputs.size(); i++) {
            System.out.printf("--- Output %d\n", i);
            System.out.printf("Output Value: %f\n\n", outputs.get(i).value);
            // System.out.println(outputs.get(i).address);
        }
    }

    /**
     * Print UTXOPool in Handler
     */
    public void printUTXOPool() {
        ArrayList<UTXO> pool = utxoPool.getAllUTXO();
        for (int i = 0; i < pool.size(); i++) {
            System.out.printf("--- UTXO %d\n", i);
            System.out.printf("Prev Hash %s\n", Arrays.toString(pool.get(i).getTxHash()));
            System.out.printf("Prev Output Val: %f\n", utxoPool.getTxOutput(pool.get(i)).value);
        }
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS
        // Keep track of claimed outputs
        // printTransaction(tx);
        // printUTXOPool();
        HashSet<UTXO> allUTXO = new HashSet<UTXO>();
        double inputSum = 0.0;
        double outputSum = 0.0;
        ArrayList<Transaction.Input> inputs = tx.getInputs();
        ArrayList<Transaction.Output> outputs = tx.getOutputs();
        // Check all inputs
        for (int i = 0; i < inputs.size(); i++) {
            Transaction.Input nowInput = inputs.get(i);
            UTXO utxo = new UTXO(nowInput.prevTxHash, nowInput.outputIndex);
            if (allUTXO.contains(utxo) == true) {
                // UTXO is claimed multiple times
                return false;
            }
            allUTXO.add(utxo);
            if (utxoPool.contains(utxo) == false) {
                // UTXO is not in the current UTXO pool
                return false;
            }
            // Get the previous output
            Transaction.Output prevOutput = utxoPool.getTxOutput(utxo);
            if (Crypto.verifySignature(prevOutput.address, tx.getRawDataToSign(i), nowInput.signature) == false) {
                return false;
            }
            inputSum += prevOutput.value;
        }
        // Check all outputs
        for (int i = 0; i < outputs.size(); i++) {
            Transaction.Output output = outputs.get(i);
            if (output.value < 0) {
                return false;
            }
            outputSum += output.value;
        }
        if (inputSum < outputSum) {
            return false;
        } 
        return true;
    }

    /**
     * Handles valid transaction and update UTXOPool
     */
    public void handleValidTx(Transaction tx) {
        ArrayList<Transaction.Input> inputs = tx.getInputs();
        ArrayList<Transaction.Output> outputs = tx.getOutputs();
        // Remove old UTXO
        for (int i = 0; i < inputs.size(); i++) {
            Transaction.Input nowInput = inputs.get(i);
            UTXO utxo = new UTXO(nowInput.prevTxHash, nowInput.outputIndex);
            // Remove original utxo
            utxoPool.removeUTXO(utxo);
        }
        // Add new UTXO
        for (int i = 0; i < outputs.size(); i++) {
            UTXO utxo = new UTXO(tx.getHash(), i);
            utxoPool.addUTXO(utxo, outputs.get(i));
        }
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        ArrayList<Transaction> validTx = new ArrayList<Transaction>();
        // Keep track of unresolved txs
        HashSet<Transaction> txs = new HashSet<Transaction>();
        for (int i = 0; i < possibleTxs.length; i++) {
            txs.add(possibleTxs[i]);
        }
        int tmpSize = txs.size();
        int newSize = tmpSize;
        do {
            tmpSize = newSize;
            // Iterate unresolved txs
            for (Iterator<Transaction> i = txs.iterator(); i.hasNext();) {
                Transaction tx = i.next();
                if (isValidTx(tx)) {
                    validTx.add(tx);
                    handleValidTx(tx);
                    i.remove();
                }
            }
            newSize = txs.size();
        } while (tmpSize != newSize);
        
        return validTx.toArray(new Transaction[validTx.size()]);
    }

    public UTXOPool getUTXOPool() {
        return utxoPool;
    }

}
