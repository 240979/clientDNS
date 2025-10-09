package models;
/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * */
import enums.*;
import javafx.scene.control.TreeItem;

public class LLMNRHeader extends Header{

    protected T t = T.NOT_TENTATIVE;
    protected C c = C.NOT_CONFLICT;

    public LLMNRHeader(int numberOfQueries, boolean conflict, boolean tentative){
        super(false, false, numberOfQueries,false,false);
        t = T.getTypeByCode(tentative);
        c = C.getTypeByCode(conflict);
    }

    public LLMNRHeader()
    {}

    /*
    * Similar to super method from super class, but slightly modified to fit LLMNR haeder
    * */
    @Override
    public Header parseHead(byte[] byteHead) {
        // id
        this.id = new UInt16().loadFromBytes(byteHead[0], byteHead[1]);

        // second byte - first in flags
        boolean[] pom1 = DataTypesConverter.byteToBoolArr(byteHead[2], 8);
        this.t = T.getTypeByCode(pom1[0]); // modified
        this.tc = TC.getTypeByCode(pom1[1]);
        this.c = C.getTypeByCode(pom1[2]); // modified
        boolean[] opcode = { pom1[3], pom1[4], pom1[5], pom1[6] };
        this.opCode = OP_CODE.getTypeByCode(DataTypesConverter.booleanArrayAsbyte(opcode));
        this.qr = QR.getTypeByCode(pom1[7]);

        // second byte in flags
        boolean[] pom2 = DataTypesConverter.byteToBoolArr(byteHead[3], 8);
        boolean[] rcodeBoolean = { pom2[0], pom2[1], pom2[2], pom2[3] };
        this.rCode = R_CODE.getTypeByCode(DataTypesConverter.booleanArrayAsbyte(rcodeBoolean));

        this.QdCount = new UInt16().loadFromBytes(byteHead[4], byteHead[5]);
        this.AnCount = new UInt16().loadFromBytes(byteHead[6], byteHead[7]);
        this.NsCount = new UInt16().loadFromBytes(byteHead[8], byteHead[9]);
        this.ArCount = new UInt16().loadFromBytes(byteHead[10], byteHead[11]);
        return this;
    }

    /*
     * Similar to super method from super class, but slightly modified to fit LLMNR haeder
     * */
    @Override
    protected TreeItem<String> getFlagsAsTreeView() {
        // added T and TC key
        String flagsKeys[] = { QR_KEY, OPCODE_KEY, CONFLICT_KEY, TC_KEY, T_KEY, RCODE_KEY };
        String flagsValue[] = { qr.toString(), opCode.toString(), c.toString(), tc.toString(), t.toString(), rCode.toString() };
        TreeItem<String> main = new TreeItem<String>("Flags");
        for (int i = 0; i < flagsValue.length; i++) {
            main.getChildren().add(new TreeItem<String>(flagsKeys[i] + ": " + flagsValue[i]));
        }
        return main;
    }
}
