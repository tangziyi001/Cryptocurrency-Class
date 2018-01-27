import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

public class BlockChain {

    public class BlockWrapper {
        
        public ByteArrayWrapper hash;
        public Block block;
        public int height;
        public UTXOPool utxoPool;
        public int serialID;
        public BlockWrapper(int serialID, ByteArrayWrapper hash, Block block, int height, UTXOPool utxoPool) {
            this.serialID = serialID;
            this.hash = hash;
            this.block = block;
            this.height = height;
            this.utxoPool = utxoPool;
        }
    }

    public static final int CUT_OFF_AGE = 10;
    public TransactionPool transactionPool;
    
    // Block hash -> Block wrapper
    public HashMap<ByteArrayWrapper, BlockWrapper> blockchain;
    
    // Height -> Block hash
    public HashMap<Integer, ArrayList<ByteArrayWrapper>> heights;
    public int blockCount;
    public int maxHeight;
    
    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        // IMPLEMENT THIS
        // Init
        blockchain = new HashMap<ByteArrayWrapper, BlockWrapper>();
        heights = new HashMap<Integer, ArrayList<ByteArrayWrapper>>();
        transactionPool = new TransactionPool();
        // Create BlockWrapper
        ByteArrayWrapper hash = new ByteArrayWrapper(genesisBlock.getHash());
        UTXOPool utxoPool = new UTXOPool();
        Transaction coinbaseTx = genesisBlock.getCoinbase();
        // For each new block, the coinbase tx's output has already been 
        // added to the transaction output list in Transaction.java Line 136
        // Add all output of the genesis block to UXTO Pool
        ArrayList<Transaction.Output> outputs = coinbaseTx.getOutputs();
        for (int i = 0; i < outputs.size(); i++) {
            UTXO utxo = new UTXO(coinbaseTx.getHash(), i);
            utxoPool.addUTXO(utxo, outputs.get(i));
        }
        BlockWrapper blockWrapper = new BlockWrapper(blockCount, hash, genesisBlock, 1, utxoPool);
        blockchain.put(hash, blockWrapper);
        // Update Height
        addToHeightMap(1, hash);
        maxHeight = 1;
        blockCount++;
    }

    /** Get the maximum height block wrapper */
    public BlockWrapper getMaxHeightBlockWrapper() {
        // Get the list of blocks with max heights
        ArrayList<ByteArrayWrapper> allBlockHash = heights.get(maxHeight);
        BlockWrapper finalBlockWrapper = blockchain.get(allBlockHash.get(0));
        int oldest = finalBlockWrapper.serialID;
        for (int i = 1; i < allBlockHash.size(); i++) {
            BlockWrapper tmpBlockWrapper = blockchain.get(allBlockHash.get(i));
            if (tmpBlockWrapper.serialID < oldest) {
                finalBlockWrapper = tmpBlockWrapper;
                oldest = tmpBlockWrapper.serialID;
            }
            // System.out.println(oldest);
        }
        return finalBlockWrapper;
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        // IMPLEMENT THIS
        return getMaxHeightBlockWrapper().block;
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        // IMPLEMENT THIS
        return getMaxHeightBlockWrapper().utxoPool;
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        // IMPLEMENT THIS
        return transactionPool;
    }

    /** Crypto class is not available for online grader, so we migrate its method here */
    public static boolean verifySignature(PublicKey pubKey, byte[] message, byte[] signature) {
        Signature sig = null;
        try {
            sig = Signature.getInstance("SHA256withRSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            sig.initVerify(pubKey);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        try {
            sig.update(message);
            return sig.verify(signature);
        } catch (SignatureException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Print UTXOPool for debug usage
     */
    public void printUTXOPool(UTXOPool utxoPool) {
        ArrayList<UTXO> pool = utxoPool.getAllUTXO();
        for (int i = 0; i < pool.size(); i++) {
            System.out.printf("--- UTXO %d\n", i);
            System.out.printf("Prev Hash %s\n", Arrays.toString(pool.get(i).getTxHash()));
            System.out.printf("Prev Output Val: %f\n", utxoPool.getTxOutput(pool.get(i)).value);
        }
    }

    /** Check the validity of a transaction */
    public boolean isValidTx(Transaction tx, UTXOPool utxoPool) {
        // IMPLEMENT THIS
        // printUTXOPool(utxoPool);
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
                // System.out.println("Tx claimed multiple times");
                return false;
            }
            allUTXO.add(utxo);
            if (utxoPool.contains(utxo) == false) {
                // UTXO is not in the current UTXO pool
                // System.out.println("Tx not in UTXO");
                return false;
            }
            // Get the previous output
            Transaction.Output prevOutput = utxoPool.getTxOutput(utxo);
            if (verifySignature(prevOutput.address, tx.getRawDataToSign(i), nowInput.signature) == false) {
                // System.out.println("Tx verification failed");
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
        // Check if inputSum >= outputSum
        if (inputSum < outputSum) {
            return false;
        } 
        return true;
    }

    /** Handle valid Tx for a block */
    public void handleValidTx(Transaction tx, UTXOPool newUTXOPool) {
        ArrayList<Transaction.Input> inputs = tx.getInputs();
        ArrayList<Transaction.Output> outputs = tx.getOutputs();
        // Remove old UTXO
        for (int i = 0; i < inputs.size(); i++) {
            Transaction.Input nowInput = inputs.get(i);
            UTXO utxo = new UTXO(nowInput.prevTxHash, nowInput.outputIndex);
            // Remove original utxo
            newUTXOPool.removeUTXO(utxo);
        }
        // Add new UTXO
        for (int i = 0; i < outputs.size(); i++) {
            UTXO utxo = new UTXO(tx.getHash(), i);
            newUTXOPool.addUTXO(utxo, outputs.get(i));
        }
    }

    /** Check transactions */
    public boolean checkTxs(ArrayList<Transaction> possibleTxs, UTXOPool newUTXOPool) {
        ArrayList<Transaction> validTx = new ArrayList<Transaction>();
        // Keep track of unresolved txs
        HashSet<Transaction> txs = new HashSet<Transaction>(possibleTxs);
        int tmpSize = txs.size();
        int newSize = tmpSize;
        // Check if all transactions can be executed
        // Do it for multiple rounds in case some transactions
        // become valid given that the other transaction is valid
        do {
            tmpSize = newSize;
            // Iterate unresolved txs
            for (Iterator<Transaction> i = txs.iterator(); i.hasNext();) {
                Transaction tx = i.next();
                if (isValidTx(tx, newUTXOPool)) {
                    validTx.add(tx);
                    handleValidTx(tx, newUTXOPool);
                    i.remove();
                }
            }
            newSize = txs.size();
        } while (tmpSize != newSize);
        // System.out.printf("%d %d\n", possibleTxs.size(), validTx.size());
        return possibleTxs.size() == validTx.size();
    }

    /** Remove outdated blocks */
    public void removeOutdatedBlocks(int outHeight) {
        if (heights.containsKey(outHeight)) {
            ArrayList<ByteArrayWrapper> list = heights.get(outHeight);
            for (int i = 0; i < list.size(); i++) {
                ByteArrayWrapper hash = list.get(i);
                blockchain.remove(hash);
            }
            heights.remove(outHeight);
        }
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     * 
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        // IMPLEMENT THIS
        // Cannot be a genesis block by assumption
        if (block.getPrevBlockHash() == null) {
            return false;
        }
        // Check if the prevBlock is outdated
        ByteArrayWrapper prevBlockHash = new ByteArrayWrapper(block.getPrevBlockHash());
        if (blockchain.containsKey(prevBlockHash) == false) {
            // Previous block has been removed because it is outdated
            return false;
        }
        // Check if block with the same hash exists
        ByteArrayWrapper hash = new ByteArrayWrapper(block.getHash());
        if (blockchain.containsKey(hash) == true) {
            // No need to process again
            // Otherwise their serial ID will be updated
            // And getMaxHeight will not correctly find the oldest block
            return true;
        }
        // Get previous utxoPool
        UTXOPool newUTXOPool = new UTXOPool(blockchain.get(prevBlockHash).utxoPool);
        
        // Handle Txs: pass UTXO pools as references
        if (checkTxs(block.getTransactions(), newUTXOPool) == false) {
            return false;
        }

        // Add coinbase transaction to the UTXO pool so it can be used
        // by following blocks
        Transaction coinbaseTx = block.getCoinbase();
        newUTXOPool.addUTXO(new UTXO(coinbaseTx.getHash(), 0), coinbaseTx.getOutput(0));

        // Add the block
        BlockWrapper newBlockWrapper = new BlockWrapper(
            blockCount, hash, block, blockchain.get(prevBlockHash).height + 1, newUTXOPool);
        blockchain.put(hash, newBlockWrapper);
        addToHeightMap(newBlockWrapper.height, hash);
        // System.out.printf("Add block successful: id %d, height %d\n", newBlockWrapper.serialID, newBlockWrapper.height);
        // System.out.printf("Prev block height %d, id %d\n", blockchain.get(prevBlockHash).height, blockchain.get(prevBlockHash).serialID);
        // Remove txs from the transaction pool
        for (int i = 0; i < block.getTransactions().size(); i++) {
            transactionPool.removeTransaction(block.getTransactions().get(i).getHash());
        }

        // Update count and max height
        blockCount++;
        if (newBlockWrapper.height > maxHeight) {
            maxHeight = newBlockWrapper.height;
            // Remove old block
            removeOutdatedBlocks(maxHeight-CUT_OFF_AGE-1);
        }
        return true;
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        // IMPLEMENT THIS
        transactionPool.addTransaction(tx);
    }

    /** Helper function for height map **/
    public void addToHeightMap(Integer h, ByteArrayWrapper b) {
        ArrayList<ByteArrayWrapper> list = heights.get(h);
        if(list == null) {
             list = new ArrayList<ByteArrayWrapper>();
             list.add(b);
             heights.put(h, list);
        } else {
            if(!list.contains(b)) list.add(b);
        }
    }
}