import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class NTTC_Blockchain {

    public static ArrayList<VNPT_Chau> blockchain = new ArrayList<VNPT_Chau>();
    public static HashMap<String,TransactionOutput> UTXOs = new HashMap<String,TransactionOutput>();

    public static int difficulty = 3;
    public static float minimumTransaction = 0.1f;
    public static Store Kho; //Kho
    public static Store CuaHang; //Kho cửa hàng
    public static Transaction genesisTransaction;

    public static void main(String[] args) {
        //add our blocks to the blockchain ArrayList:
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider()); //Thiết lập bảo mật bằng phương thức BouncyCastleProvider

        //Create Mobiles:
        Kho = new Store();
        CuaHang = new Store();
        int Kho_VNPT ;
        int CuaHang_VNPT ;
        int x;
        int i= 0;
        Scanner scanner = new Scanner(System.in);
        System.out.println("So luong VNPT-Net Router trong kho:");
        Kho_VNPT = scanner.nextInt();
        System.out.println("So luong VNPT-Net Router cua hang dang con:");
        CuaHang_VNPT = scanner.nextInt();
        System.out.println("Nhap so luong VNPT-Net Router chuyen tu kho sang cua hang:");
        x = scanner.nextInt();
        while(x>Kho_VNPT) {  
            System.out.println("Vuot qua so luong VNPT-Net Router trong kho, vui long nhap lai!!!!!");
             x = scanner.nextInt();
        } 
        Store coinbase = new Store();

        //Khởi tạo giao dịch gốc

        genesisTransaction = new Transaction(coinbase.publicKey, Kho.publicKey, Kho_VNPT, null);
        genesisTransaction.generateSignature(coinbase.privateKey);	 //Gán private key (ký thủ công) vào giao dịch gốc
        genesisTransaction.transactionId = "0"; //Gán ID cho giao dịch gốc
        genesisTransaction.outputs.add(new TransactionOutput(genesisTransaction.reciepient, genesisTransaction.value, genesisTransaction.transactionId)); //Thêm Transactions Output
        UTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0)); //Lưu giao dịch đầu tiên vào danh sách UTXOs.

        genesisTransaction = new Transaction(coinbase.publicKey, CuaHang.publicKey, CuaHang_VNPT, null);
        genesisTransaction.generateSignature(coinbase.privateKey);	 //Gán private key (ký thủ công) vào giao dịch gốc
        genesisTransaction.transactionId = "0"; //Gán ID cho giao dịch gốc
        genesisTransaction.outputs.add(new TransactionOutput(genesisTransaction.reciepient, genesisTransaction.value, genesisTransaction.transactionId)); //Thêm Transactions Output
        UTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0)); //Lưu giao dịch đầu tiên vào danh sách UTXOs.

        System.out.println("Dang tao khoi.... ");
        VNPT_Chau genesis = new VNPT_Chau("0");
        genesis.addTransaction(genesisTransaction);
        addBlock(genesis);

        //Thử nghiệm
        VNPT_Chau block1 = new VNPT_Chau(genesis.hash);
        System.out.println("\nSo luong VNPT-Net Router trong kho la : " + Kho.getBalance());
        System.out.println("\nSo luong VNPT-Net Router tai cua hang la : " + CuaHang.getBalance());
        System.out.println("\nSo luong VNPT-Net Router chuyen tu kho sang cua hang...");
        block1.addTransaction(Kho.sendFunds(CuaHang.publicKey, x));
        addBlock(block1);
        System.out.println("\nSo luong VNPT-Net Router con lai la: " + Kho.getBalance());
        System.out.println("So luong VNPT-Net Router hien co cua cua hang la: " + CuaHang.getBalance());
        isChainValid();

    }

    public static Boolean isChainValid() {
        VNPT_Chau currentBlock;
        VNPT_Chau previousBlock;
        String hashTarget = new String(new char[difficulty]).replace('\0', '0');
        HashMap<String,TransactionOutput> tempUTXOs = new HashMap<String,TransactionOutput>(); //Tạo một danh sách hoạt động tạm thời của các giao dịch chưa được thực thi tại một trạng thái khối nhất định.
        tempUTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));

        //loop through blockchain to check hashes:
        for(int i=1; i < blockchain.size(); i++) {

            currentBlock = blockchain.get(i);
            previousBlock = blockchain.get(i-1);
            //Kiểm tra, so sánh mã băm đã đăng ký với mã băm được tính toán
            if(!currentBlock.hash.equals(currentBlock.calculateHash()) ){
                System.out.println("#Ma bam hien tai khong khop");
                return false;
            }
            //So sánh mã băm của khối trước với mã băm của khối trước đã được đăng ký
            if(!previousBlock.hash.equals(currentBlock.previousHash) ) {
                System.out.println("#Ma bam khoi truoc khong khop");
                return false;
            }
            //Kiểm tra xem mã băm có lỗi không
            if(!currentBlock.hash.substring( 0, difficulty).equals(hashTarget)) {
                System.out.println("#Khoi nay khong dao duoc do loi!");
                return false;
            }

            //Vòng lặp kiểm tra các giao dịch
            TransactionOutput tempOutput;
            for(int t=0; t < currentBlock.transactions.size(); t++) {
                Transaction currentTransaction = currentBlock.transactions.get(t);

                if(!currentTransaction.verifySignature()) {
                    System.out.println("#Chu ky so cua giao dich (" + t + ") khong hop le");
                    return false;
                }
                if(currentTransaction.getInputsValue() != currentTransaction.getOutputsValue()) {
                    System.out.println("#cac dau vao khong khop voi dau ra trong giao dich (" + t + ")");
                    return false;
                }

                for(TransactionInput input: currentTransaction.inputs) {
                    tempOutput = tempUTXOs.get(input.transactionOutputId);

                    if(tempOutput == null) {
                        System.out.println("#Cac dau vao tham chieu trong giao dich (" + t + ") bi thieu!");
                        return false;
                    }

                    if(input.UTXO.value != tempOutput.value) {
                        System.out.println("#Cac dau vao tham chieu trong giao dich (" + t + ") co gia tri khong hop le");
                        return false;
                    }

                    tempUTXOs.remove(input.transactionOutputId);
                }

                for(TransactionOutput output: currentTransaction.outputs) {
                    tempUTXOs.put(output.id, output);
                }

                if( currentTransaction.outputs.get(0).reciepient != currentTransaction.reciepient) {
                    System.out.println("#Giao dich(" + t + ") co nguoi nhan khong dung!");
                    return false;
                }
                if( currentTransaction.outputs.get(1).reciepient != currentTransaction.sender) {
                    System.out.println("#Dau ra cua giao dich (" + t + ") khong dung voi nguoi gui.");
                    return false;
                }

            }

        }
        System.out.println("Chuoi khong hop le!");
        return true;
    }

    public static void addBlock(VNPT_Chau newBlock) {
        newBlock.mineBlock(difficulty);
        blockchain.add(newBlock);
    }
}
