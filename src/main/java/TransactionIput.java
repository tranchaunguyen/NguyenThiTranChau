public class TransactionInput {

    public String transactionOutputId;
    public TransactionOutput UTXO; // Unspent Transaction Output

    public TransactionInput(String transactionOutputId) {
        this.transactionOutputId = transactionOutputId;
    }
}
