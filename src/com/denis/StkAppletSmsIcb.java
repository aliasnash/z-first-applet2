package com.denis;

import javacard.framework.AID;
import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.Shareable;
import javacard.framework.SystemException;
import javacard.framework.Util;
import sim.access.SIMSystem;
import sim.access.SIMView;
import sim.access.SIMViewException;
import sim.toolkit.EnvelopeHandler;
import sim.toolkit.ProactiveHandler;
import sim.toolkit.ProactiveResponseHandler;
import sim.toolkit.ToolkitConstants;
import sim.toolkit.ToolkitInterface;
import sim.toolkit.ToolkitRegistry;

public class StkAppletSmsIcb extends Applet implements Shareable, ToolkitInterface, ToolkitConstants, VarsSmsIcb {
    
    public static final byte[]  AIDSequence                             = new byte[] { (byte) -96, (byte) 0, (byte) 0, (byte) 0, (byte) 9, (byte) 0, (byte) 1 };
    public static final byte[]  imeiBuffer                              = new byte[] { (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0 };
    private static final byte[] imeiBufferToCompare                     = new byte[] { (byte) -17, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1, (byte) -1 };
    private static final byte[] hameleonString                          = new byte[] { (byte) 8, (byte) 104, (byte) 97, (byte) 109, (byte) 101, (byte) 108, (byte) 101, (byte) 111, (byte) 110 };
    private static final byte[] appletVersion                           = new byte[] { (byte) 51, (byte) 46, (byte) 49, (byte) 46, (byte) 54, (byte) 46, (byte) 49 };
    
    private static short        DF_CELLTICK                             = (short) 0xAAAC;
    private static short        EF_APP_MESSAGES                         = (short) 0x2E20;
    private static short        EF_SERVICE_NAME                         = (short) 0x2E21;
    
    private static byte         CELL_BROADCAST                          = 1;
    private static byte         SMS_PP                                  = 2;
    private static byte         MENU_SELECTION                          = 3;
    
    private static final byte   SMS_SENDED                              = 2;
    private static final byte   READY                                   = 3;
    
    private static byte         flowState                               = READY;
    
    private static final byte   TAG_BASE_ITEM_NEXT                      = 0x64;
    private static final byte   TAG_BASE_ITEM_MAIN                      = 0x65;
    private static final byte   TAG_BASE_ITEM_CALL                      = 0x01;
    private static final byte   TAG_BASE_ITEM_DETAIL                    = 0x02;
    private static final byte   TAG_BASE_ITEM_YES                       = 0x03;
    private static final byte   TAG_BASE_ITEM_NO                        = 0x04;
    
    private static boolean      isAllowPlaySound                        = true;
    
    public short                smsUserDataOffset;
    private boolean             isAddressInSmsTpduExist                 = false;
    private byte[]              byteBuffer;
    public short[]              shortBuffer;
    private boolean[]           concatMsgMapper;
    public boolean              serviceInProgress;
    private byte                userDataMessageReference                = -1;
    public boolean              msgLimitExceed;
    private byte                firstByteMarkerClass                    = 0;
    private byte                concatMsgCounter;
    public byte                 countMsgLimitExceed                     = 0;
    
    private static short        CHANNEL_1                               = 0x2610;
    private static short        CHANNEL_2                               = 0x1026;
    private static byte         dcs;
    
    // номер в холдере где храниться адрес на симки куда писать кол-во итемсов
    private static final byte   BASE_ITEMS_LENGTH_OFFSET                = 0;
    // сдвиг в буфере
    private static final byte   BASE_ITEMS_OFFSET                       = 1;
    private static byte         baseItemsCount;
    
    private static boolean      imeiFlag;
    // private boolean field_token24_descoff842;
    private boolean             moreCycles;
    
    // У с л у г а в к л ю ч е н а
    private static byte         PROMPT_INDEX_SERVICE_ON                 = 1;
    // В в е д и т е т е к с т :
    private static byte         PROMPT_INDEX_INPUT_TEXT                 = 2;
    // В в е д и т е н о м е р :
    private static byte         PROMPT_INDEX_INPUT_NUMBER               = 3;
    // С о о б щ е н и е с о х р а н е н о
    private static byte         PROMPT_INDEX_MESSAGE_SAVED              = 4;
    // Т е л е ф о н н е п о д д е р ж и в а е т д а н н у ю у с л у г у
    private static byte         PROMPT_INDEX_PHONE_NOT_SUPPORT          = 7;
    // В а ш з а п р о с н е м о ж е т б ы т ь в ы п о л н е н
    private static byte         PROMPT_INDEX_REQUEST_NOT_DONE           = 8;
    // В ы к л ю ч и т е , з а т е м в к л ю ч и т е т е л е ф о н .
    private static byte         PROMPT_INDEX_RESTART_PHONE              = 29;
    // Д л я с о х р а н е н и я э т о г о с о о б щ е н и я с а м ы е с т а р ы е в а р х и в е б у д у т у д а л е н ы . С о г л а с н ы ?
    private static byte         PROMPT_INDEX_REMOVE_MESSAGES            = 35;
    
    private static short        icbMsgRefsCounter;
    
    // field_token29_descoff877;
    private byte                shortNumber1LenGlobal                   = 0;
    // field_token30_descoff884
    private byte                shortNumber2LenGlobal                   = 0;
    
    /* UNKNOWN VARS */
    
    public boolean[]            field_token15_descoff779;
    
    private static short        sfield_token255_descoff653_staticref117 = 2;
    
    private boolean             field_token27_descoff863;
    
    private static byte         sfield_token255_descoff268_staticref56;
    private static short        sfield_token255_descoff142_staticref34;
    
    private static boolean      sfield_token255_descoff191_staticref42;
    private static byte         sfield_token255_descoff303_staticref63  = 4;
    
    private static boolean      sfield_token255_descoff639_staticref114 = true;
    public final byte           field_token2_descoff688                 = 6;
    private byte                field_token25_descoff849                = 0;
    
    public final byte           field_token7_descoff723                 = 1;
    public final byte           field_token14_descoff772                = 5;
    private static byte         sfield_token255_descoff128_staticref32;
    private static boolean      sfield_token255_descoff177_staticref40;
    private static byte         sfield_token255_descoff212_staticref45;
    public final byte           field_token5_descoff709                 = 2;
    public final byte           field_token12_descoff758                = 3;
    private static boolean      sfield_token255_descoff632_staticref113 = true;
    public final byte           field_token1_descoff681                 = 3;
    
    private boolean             field_token31_descoff891                = false;
    private static short        sfield_token255_descoff247_staticref50;
    
    private static byte         sfield_token255_descoff170_staticref39;
    private static byte         sfield_token255_descoff331_staticref67  = 1;
    private static short        sfield_token255_descoff667_staticref121 = 1;
    
    public final byte           field_token4_descoff702                 = 1;
    public final byte           field_token11_descoff751                = 2;
    
    public final byte           field_token9_descoff737                 = 0;
    public byte[]               field_token16_descoff786;
    
    private static short        sfield_token255_descoff660_staticref119 = 1;
    
    private boolean             field_token28_descoff870;
    
    private static byte         sfield_token255_descoff310_staticref64  = 2;
    private static byte         sfield_token255_descoff317_staticref65  = 1;
    
    private static short        sfield_token255_descoff646_staticref115 = 4;
    public final byte           field_token3_descoff695                 = 0;
    
    private short               field_token26_descoff856                = 0;
    
    public final byte           field_token8_descoff730                 = 2;
    private static boolean      sfield_token255_descoff135_staticref33;
    private static boolean      sfield_token255_descoff184_staticref41;
    
    public final byte           field_token6_descoff716                 = 0;
    public final byte           field_token13_descoff765                = 4;
    
    private static byte         sfield_token255_descoff205_staticref44;
    private static short        sfield_token255_descoff254_staticref52;
    
    public final byte           field_token0_descoff674                 = 3;
    
    private short               parsedMsgBufferOffset                   = -6;
    
    private static byte         sfield_token255_descoff163_staticref38;
    public final byte           field_token10_descoff744                = 1;
    private static boolean      sfield_token255_descoff149_staticref36;
    
    public StkAppletSmsIcb() {
        ToolkitRegistry reg = ToolkitRegistry.getEntry();
        
        try {
            this.byteBuffer = JCSystem.makeTransientByteArray((short) 41, JCSystem.CLEAR_ON_RESET);
            this.concatMsgMapper = JCSystem.makeTransientBooleanArray((short) 4, JCSystem.CLEAR_ON_RESET);
            this.field_token15_descoff779 = JCSystem.makeTransientBooleanArray((short) 3, JCSystem.CLEAR_ON_RESET);
            this.field_token16_descoff786 = JCSystem.makeTransientByteArray((short) 3, JCSystem.CLEAR_ON_RESET);
            this.shortBuffer = JCSystem.makeTransientShortArray((short) 6, JCSystem.CLEAR_ON_RESET);
        } catch (SystemException ex) {}
        
        try {
            SIMView sv = SIMSystem.getTheSIMView();
            sv.select(SIMView.FID_MF);
            sv.select(SIMView.FID_DF_GSM);
            sv.select(DF_CELLTICK);
            sv.select(EF_SERVICE_NAME);
            sv.readBinary((short) 0, this.byteBuffer, (short) 0, (short) 1);
            if (this.byteBuffer[0] > 0 && this.byteBuffer[0] <= 27) {
                sv.readBinary((short) 1, this.byteBuffer, (short) 1, this.byteBuffer[0]);
            } else {
                Util.arrayCopy(hameleonString, (short) 0, this.byteBuffer, (short) 0, (short) (hameleonString[0] + 1));
            }
        } catch (SIMViewException var2) {
            Util.arrayCopy(hameleonString, (short) 0, this.byteBuffer, (short) 0, (short) (hameleonString[0] + 1));
        }
        
        reg.initMenuEntry(this.byteBuffer, (short) 1, this.byteBuffer[0], (byte) 0, false, (byte) 1, (short) 0);
        reg.setEvent(EVENT_UNFORMATTED_SMS_PP_ENV);
        reg.setEvent(EVENT_PROFILE_DOWNLOAD);
        reg.setEvent(EVENT_UNFORMATTED_SMS_CB);
    }
    
    public void processToolkit(byte event) {
        try {
            switch (event) {
                case EVENT_PROFILE_DOWNLOAD:
                    this.profileDownload();
                case EVENT_FORMATTED_SMS_PP_ENV:
                case EVENT_FORMATTED_SMS_PP_UPD:
                case EVENT_UNFORMATTED_SMS_PP_UPD:
                default:
                    break;
                case EVENT_UNFORMATTED_SMS_PP_ENV:
                    if (this.serviceInProgress) {
                        return;
                    }
                    
                    this.serviceInProgress = true;
                    ProactiveHandler proHandl = ProactiveHandler.getTheHandler();
                    proHandl.init(PRO_CMD_MORE_TIME, (byte) 0, DEV_ID_ME);
                    proHandl.send();
                    
                    this.eventSmsPPDataDownload();
                    this.serviceInProgress = false;
                    break;
                case EVENT_UNFORMATTED_SMS_CB:
                    if (this.serviceInProgress) {
                        return;
                    }
                    
                    this.serviceInProgress = true;
                    this.processCellBroadcastPage();
                    this.serviceInProgress = false;
                    break;
                // case EVENT_MENU_SELECTION:
                // if (this.serviceInProgress) {
                // return;
                // }
                // this.serviceInProgress = true;
                // this.eventMenuSelection();
                // this.serviceInProgress = false;
            }
        } catch (Exception var3) {
            this.serviceInProgress = false;
            this.resetVars();
            sfield_token255_descoff128_staticref32 = 0;
            sfield_token255_descoff135_staticref33 = false;
            flowState = READY;
        }
        
    }
    
    public static void install(byte[] bArray, short bOffset, byte bLength) {
        StkAppletSmsIcb sa = new StkAppletSmsIcb();
        
        try {
            sa.register();
        } catch (Exception ex) {
            sa.register(bArray, (short) ((short) bOffset + 1), bArray[(short) bOffset]);
        }
    }
    
    public void process(APDU apdu) {
        ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
    }
    
    public boolean select() {
        return true;
    }
    
    public Shareable getShareableInterfaceObject(AID clientAID, byte parameter) {
        return clientAID.partialEquals(AIDSequence, (short) 0, (byte) AIDSequence.length) ? this : null;
    }
    
    /*
     * DIFF METHODS *** DIFF METHODS *** DIFF METHODS *** DIFF METHODS *** DIFF METHODS *** DIFF METHODS
     * DIFF METHODS *** DIFF METHODS *** DIFF METHODS *** DIFF METHODS *** DIFF METHODS *** DIFF METHODS
     * DIFF METHODS *** DIFF METHODS *** DIFF METHODS *** DIFF METHODS *** DIFF METHODS *** DIFF METHODS
     * DIFF METHODS *** DIFF METHODS *** DIFF METHODS *** DIFF METHODS *** DIFF METHODS *** DIFF METHODS
     * 
     */
    
    private void profileDownload() {
        sfield_token255_descoff128_staticref32 = 0;
        
        this.serviceInProgress = false;
        this.initAppletBuffers();
        
        this.resetIcbMsgRefsHolder();
        this.resetVars();
        // sfield_token255_descoff296_staticref62 = sfield_token255_descoff289_staticref61;
        sfield_token255_descoff135_staticref33 = false;
        flowState = READY;
        if (sfield_token255_descoff639_staticref114) {
            if (!imeiFlag) {
                this.getImeiFromME();
                imeiFlag = true;
                
                if (Util.arrayCompare(imeiBuffer, (short) 0, imeiBufferToCompare, (short) 0, (short) 8) != 0) {
                    sfield_token255_descoff317_staticref65 = sfield_token255_descoff310_staticref64;
                    Util.arrayCopy(imeiBuffer, (short) 0, imeiBufferToCompare, (short) 0, (short) 8);
                    sfield_token255_descoff310_staticref64 = 2;
                    sfield_token255_descoff177_staticref40 = false;
                    if (sfield_token255_descoff632_staticref113) {
                        sfield_token255_descoff632_staticref113 = false;
                        sfield_token255_descoff247_staticref50 = sfield_token255_descoff254_staticref52 = sfield_token255_descoff646_staticref115;
                    }
                    
                    this.workWithICBchannel(true);
                } else {
                    imeiFlag = false;
                    sfield_token255_descoff632_staticref113 = true;
                }
            } else {
                imeiFlag = false;
            }
        }
    }
    
    // method_token255_descoff1529
    /* вызывается только с одного места, с processShowMessageAndParseResult */
    private void copyBufferToParsedMessage(boolean isCyrilic, byte[] buffer, short bufferOffset, short maxLength) {
        short len = 0;
        short msgOffset = 601;
        
        if (isCyrilic) {
            PARSED_MESSAGE[msgOffset++] = (byte) 0x80; // -128
            
            for (byte i = 0; i < 20; i = (byte) (i + 2)) {
                PARSED_MESSAGE[msgOffset + i] = 0x00;
                PARSED_MESSAGE[msgOffset + i + 1] = 0x20;
            }
            
            len = (short) (maxLength < 20 ? maxLength : 20);
        } else {
            for (byte i = 0; i < 21; i++) {
                PARSED_MESSAGE[msgOffset + i] = 0x20;
            }
            
            len = (short) (maxLength < 10 ? maxLength : 10);
        }
        
        PARSED_MESSAGE[600] = (byte) (len + (isCyrilic ? 1 : 0));
        Util.arrayCopy(buffer, bufferOffset, PARSED_MESSAGE, msgOffset, len);
    }
    
    // method_token255_descoff1013
    private byte displayText(byte dcs, byte[] text, short offset, short length) {
        ProactiveHandler proHandlr = ProactiveHandler.getTheHandler();
        proHandlr.initDisplayText((byte) 0x80, dcs, text, offset, length); // byte qualifier, byte dcs, byte[] buffer, short offset, short length
        return proHandlr.send();
    }
    
    // method_token255_descoff1577
    private boolean checkIcbChannel(byte[] buffer, byte index) {
        short value = Util.getShort(buffer, index);
        return value == CHANNEL_2 || value == CHANNEL_1;
    }
    
    // method_token255_descoff1061
    private void buildImeiAndVersionBuffer() {
        /*
         * 00-12 : first number from bufferForNumbers
         * 13 : length of data followed
         * 14 : 73
         * 15 : 77
         * 16 : 69
         * 17 : 73
         * 18 : 32
         * 19-33 : IMEI
         * 34 : 32
         * 35-41 : version of applet
         */
        short offset = Util.arrayCopy(BUFFER_FOR_NUMBERS, (short) 0, EXTENDED_BUFFER, (short) 0, (short) (BUFFER_FOR_NUMBERS[0] + 1)); // 13
        short offsetSaved = offset++; // 13
        
        EXTENDED_BUFFER[offset++] = 73; // 14
        EXTENDED_BUFFER[offset++] = 77; // 15
        EXTENDED_BUFFER[offset++] = 69; // 16
        EXTENDED_BUFFER[offset++] = 73; // 17
        EXTENDED_BUFFER[offset++] = 32; // 18
        Util.arrayCopy(imeiBuffer, (short) 0, this.byteBuffer, (short) 0, (short) imeiBuffer.length);
        
        for (byte i = (byte) (8 - 1); i >= 0; i--) {
            this.byteBuffer[(byte) (i * 2 + 1)] = this.processImeiDigit((byte) ((this.byteBuffer[i] >> 4) & 15));
            this.byteBuffer[(byte) (i * 2)] = this.processImeiDigit((byte) (this.byteBuffer[i] & 15));
        }
        
        for (byte i = 0; i <= 15; i++) {
            this.byteBuffer[i] = this.byteBuffer[(byte) (i + 1)];
        }
        
        offset = Util.arrayCopy(this.byteBuffer, (short) 0, EXTENDED_BUFFER, offset, (short) 15); // 34
        EXTENDED_BUFFER[offset++] = 32; // 34
        
        offset = Util.arrayCopy(appletVersion, (short) 0, EXTENDED_BUFFER, offset, (short) appletVersion.length);
        EXTENDED_BUFFER[offsetSaved] = (byte) (offset - offsetSaved - 1);
    }
    
    private byte processImeiDigit(byte digit) {
        if (digit < 10) {
            return (byte) (digit + 48);
        } else {
            return (byte) (digit + 55);
        }
    }
    
    private boolean method_token255_descoff989(byte[] var1, short var2, boolean var3) {
        short var4 = (short) (var1 == sfield_token255_descoff114_staticref28 ? sfield_token255_descoff660_staticref119 : sfield_token255_descoff667_staticref121);
        short var5 = (short) 1;
        short var6 = (short) 0;
        short var7 = (short) 0;
        short var8 = (short) 0;
        
        short var9;
        for (var9 = (short) var1[0]; (short) var8 < (short) var9; ++var8) {
            var6 = (short) var1[(short) var5];
            var7 = (short) Util.getShort(var1, (short) ((short) ((short) var5 + 1) + (short) var6));
            if (var3 && (short) ((short) ((short) var5 + (short) var6) + 1) == (short) var2) {
                break;
            }
            
            var5 = (short) ((short) ((short) var5 + (short) ((short) ((short) ((short) var6 + (short) var7) + 1) + 2)));
            if (!var3 && (short) ((short) var5 - 1) >= (short) var2) {
                break;
            }
        }
        
        if ((short) var8 == (short) var9) {
            return false;
        } else {
            if (var3) {
                var9 = (short) 1;
                var8 = (short) ((short) ((short) ((short) var2 + (short) var7) + 2));
            } else {
                var9 = (short) ((short) ((short) var8 + 1));
                var8 = (short) ((short) var5);
                var5 = (short) 1;
            }
            
            if ((short) var9 < var1[0]) {
                Util.arrayCopyNonAtomic(var1, (short) var8, var1, (short) var5, (short) ((short) var4 - (short) var8));
            }
            
            var4 = (short) ((short) ((short) var4 - (short) ((short) var8 - (short) var5)));
            if (var1 == sfield_token255_descoff114_staticref28) {
                sfield_token255_descoff660_staticref119 = (short) var4;
            } else {
                sfield_token255_descoff667_staticref121 = (short) var4;
            }
            
            var1[0] = (byte) ((short) (var1[0] - (byte) ((short) var9)));
            return true;
        }
    }
    
    // method_token255_descoff1469
    private boolean processBufferAndSendSMS(byte[] buffer, short index, byte dcsValue, byte validatePeriod) {
        short offset = 0;
        tmpMessageBuffer[offset++] = 0x11; // 0x11 0001:0001 (TP-MTI bit 0 and bit 1 - submit type) (TP-VPF bit 3 and bit 4 - relative format)
        tmpMessageBuffer[offset++] = (byte) (getSmsMessageReferece() + 1); // TP-MR msg reference
        
        short len = (short) (buffer[index++] & 0xFF);
        
        // 1. first 1 byte that inform the length of address
        // 2. 2nd 1 byte that inform TON/NPI
        // 3. the rest is address in BCD swapped format
        offset = Util.arrayCopy(buffer, index, tmpMessageBuffer, offset, len); // TP-DA
        index = (short) (index + len);
        
        tmpMessageBuffer[offset++] = 0x00; // TP-PID
        tmpMessageBuffer[offset++] = dcsValue; // TP-DCS
        tmpMessageBuffer[offset++] = validatePeriod; // TP-VP
        
        len = buffer[index++];
        short savedOffset = offset;
        
        // if (dcsValue == DCS_8_BIT_DATA && sfield_token255_descoff219_staticref46) {
        // tmpMessageBuffer[offset++] = 0x00;
        // tmpMessageBuffer[offset++] = 0x4D;
        // tmpMessageBuffer[offset++] = 0x43;
        // tmpMessageBuffer[offset++] = 0x20;
        // tmpMessageBuffer[offset++] = 0x02;
        // tmpMessageBuffer[offset++] = (byte) len; // TP-UDL
        // } else {
        tmpMessageBuffer[offset++] = (byte) len; // TP-UDL
        // }
        
        offset = Util.arrayCopy(buffer, index, tmpMessageBuffer, offset, len); // TP-UD
        
        // if (dcsValue == DCS_8_BIT_DATA && sfield_token255_descoff219_staticref46) {
        // tmpMessageBuffer[offset++] = (byte) (4 + sfield_token255_descoff212_staticref45 * 2);
        // tmpMessageBuffer[offset++] = 0x50;
        // tmpMessageBuffer[offset++] = 0x43;
        // tmpMessageBuffer[offset++] = 0x20;
        // tmpMessageBuffer[offset++] = sfield_token255_descoff212_staticref45;
        // offset = Util.arrayCopy(sfield_token255_descoff44_staticref8, (short) 0, tmpMessageBuffer, offset, (short)
        // (sfield_token255_descoff212_staticref45 * 2));
        // }
        
        tmpMessageBuffer[savedOffset] = (byte) (offset - savedOffset - 1); // TP-UDL
        
        sfield_token255_descoff268_staticref56 = this.method_token255_descoff1037(tmpMessageBuffer, (short) (savedOffset + 1), tmpMessageBuffer[savedOffset]);
        
        ProactiveHandler proHandl = ProactiveHandler.getTheHandler();
        // qualifier
        // bit 1:
        // 0 = packing not required;
        // 1 = SMS packing by the ME required.
        // bits 2 to 8: = 0 RFU.
        byte qualifier = (byte) (dcsValue == DCS_DEFAULT_ALPHABET ? 1 : 0);
        
        proHandl.init(PRO_CMD_SEND_SHORT_MESSAGE, qualifier, DEV_ID_NETWORK);
        proHandl.appendTLV(TAG_SMS_TPDU, tmpMessageBuffer, (short) 0, offset);
        byte result = proHandl.send();
        if (result == RES_CMD_PERF) {
            // sfield_token255_descoff219_staticref46 = false;
            
            updateSmsMessageReferece(tmpMessageBuffer[1]);
            return true;
        } else {
            return false;
        }
    }
    
    // method_token255_descoff1457
    private void composeAndSendSMS(byte[] message, short messageOffset, byte[] buffer, short bufferOffset) {
        short offset = (short) 0;
        
        tmpMessageBuffer[offset++] = 0x11; // 0x11 0001:0001 (TP-MTI bit 0 and bit 1 - submit type) (TP-VPF bit 3 and bit 4 - relative format)
        tmpMessageBuffer[offset++] = (byte) (getSmsMessageReferece() + 1); // TP-MR msg reference
        
        byte len = (byte) (message[messageOffset++] & 0xFF);
        
        // 1. first 1 byte that inform the length of address
        // 2. 2nd 1 byte that inform TON/NPI
        // 3. the rest is address in BCD swapped format
        offset = Util.arrayCopy(message, messageOffset, tmpMessageBuffer, offset, len); // TP-DA
        messageOffset = (short) (messageOffset + len);
        
        tmpMessageBuffer[offset++] = 0x00; // TP-PID
        tmpMessageBuffer[offset++] = 0x04; // TP-DCS
        tmpMessageBuffer[offset++] = 0x00; // TP-VP
        
        len = (byte) (message[messageOffset++] & 0xFF);
        short savedOffset = offset;
        
        tmpMessageBuffer[offset++] = len;
        offset = Util.arrayCopy(message, messageOffset, tmpMessageBuffer, offset, len);
        
        byte lenOfBufferData = (byte) (buffer[bufferOffset] & 255);
        if (len + lenOfBufferData > 140) {
            lenOfBufferData = (byte) (140 - len);
        }
        
        offset = Util.arrayCopy(buffer, (short) (bufferOffset + 1), tmpMessageBuffer, offset, lenOfBufferData);
        tmpMessageBuffer[savedOffset] = (byte) (lenOfBufferData + len); // TP-UDL
        
        sfield_token255_descoff268_staticref56 = this.method_token255_descoff1037(tmpMessageBuffer, (byte) (savedOffset + 1), tmpMessageBuffer[savedOffset]);
        
        ProactiveHandler proHandl = ProactiveHandler.getTheHandler();
        proHandl.init(PRO_CMD_SEND_SHORT_MESSAGE, (byte) 0, DEV_ID_NETWORK);
        proHandl.appendTLV(TAG_SMS_TPDU, tmpMessageBuffer, (short) 0, offset);
        byte result = (byte) (proHandl.send() & 0xFF);
        if (result == RES_CMD_PERF) {
            updateSmsMessageReferece(tmpMessageBuffer[1]);
        }
    }
    
    private byte getSmsMessageReferece() {
        byte[] data = { 0 };
        
        try {
            SIMView simView = SIMSystem.getTheSIMView();
            simView.select(SIMView.FID_MF);
            simView.select(SIMView.FID_DF_TELECOM);
            simView.select(SIMView.FID_EF_SMSS);
            simView.readBinary((short) 0, data, (short) 0, (short) 1);
        } catch (SIMViewException ex) {
            data[0] = 1;
        }
        
        return data[0];
    }
    
    private void updateSmsMessageReferece(byte mr) {
        byte[] data = { mr };
        
        try {
            SIMView simView = SIMSystem.getTheSIMView();
            simView.select(SIMView.FID_MF);
            simView.select(SIMView.FID_DF_TELECOM);
            simView.select(SIMView.FID_EF_SMSS);
            simView.updateBinary((short) 0, data, (short) 0, (short) 1);
        } catch (SIMViewException ex) {}
    }
    
    private boolean method_token255_descoff941(byte var1) {
        switch ((short) var1) {
            case -128:
            case 0:
            case 16:
            case 32:
            case 48:
            case 64:
            case 80:
            case 96:
            case 112:
                return false;
            case -112:
                return true;
            default:
                return false;
        }
    }
    
    // method_token255_descoff1025
    /* вызывается только с одного места, с processDataMessage */
    private void processShowMessageAndParseResult(boolean isCyrilic, byte var2, short var3, boolean var4, boolean isSmsSource) {
        short maxLength = 0;
        short bufferMessageOffset = var3;
        short var9 = (short) 0;
        short var10 = (short) 0;
        short var11 = (short) 0;
        short var12 = (short) 2;
        byte displayTextRes = 0;
        this.parsedMsgBufferOffset = -6;
        byte dataCoding = (byte) (isCyrilic ? 8 : 4);
        
        var9 = (short) EXTENDED_BUFFER[bufferMessageOffset++];
        if ((short) var9 >= 1) {
            for (var11 = 0; var11 != var2; var11++) {
                var10 = (short) 0;
                if ((short) var11 == 127) {
                    var11 = (short) 0;
                }
                
                bufferMessageOffset = (short) (var3 + 1);
                
                for (short var14 = (short) 0; (short) var14 < (short) var9; var14 = (short) ((byte) ((short) ((short) var14 + 1)))) {
                    this.field_token15_descoff779[1] = (short) var14 == (short) ((short) var9 - 1);
                    var12 = (short) EXTENDED_BUFFER[bufferMessageOffset++];
                    Util.arrayCopy(EXTENDED_BUFFER, bufferMessageOffset, this.byteBuffer, (short) 0, (short) var12);
                    if (this.field_token25_descoff849 != 2 && this.field_token25_descoff849 != 4) {
                        maxLength = (short) Util.makeShort((byte) 0, this.byteBuffer[(short) ((short) var12 - 1)]);
                    } else {
                        maxLength = (short) Util.getShort(PARSED_MESSAGE, (short) (622 + (short) ((short) var10 * 2)));
                        var10 = (short) ((byte) ((short) ((short) var10 + 1)));
                    }
                    
                    bufferMessageOffset = (short) (bufferMessageOffset + var12);
                    if (isSmsSource || this.field_token31_descoff891) {
                        this.copyBufferToParsedMessage(isCyrilic, EXTENDED_BUFFER, bufferMessageOffset, maxLength);
                        this.field_token31_descoff891 = false;
                        if (isSmsSource) {
                            this.method_token255_descoff1517(sfield_token255_descoff114_staticref28, this.shortBuffer[0], this.shortBuffer[4], false);
                        }
                    }
                    
                    if (!var4) {
                        return;
                    }
                    
                    isSmsSource = false;
                    if (isCyrilic) {
                        displayTextRes = this.displayText(dataCoding, EXTENDED_BUFFER, bufferMessageOffset, maxLength > 159 ? 158 : maxLength);
                    } else {
                        displayTextRes = this.displayText(dataCoding, EXTENDED_BUFFER, bufferMessageOffset, maxLength > 159 ? 159 : maxLength);
                    }
                    
                    if (displayTextRes != RES_CMD_PERF_NO_RESP_FROM_USER) {
                        this.processResultOfDisplayText(displayTextRes);
                        if (this.parsedMsgBufferOffset != -6) {
                            return;
                        }
                    } else {
                        var14 = (short) ((short) var9);
                        this.parsedMsgBufferOffset = -4;
                    }
                    
                    bufferMessageOffset = (short) (bufferMessageOffset + maxLength);
                }
            }
            
        }
    }
    
    // method_token255_descoff11811
    /* вызывается только с одного места, с processShowMessageAndParseResult */
    private void processResultOfDisplayText(byte displayTextRes) {
        // if ((short) var1 == 16) {
        // this.field_token21_descoff821 = true;
        // }
        
        switch (displayTextRes) {
            case RES_CMD_PERF:
                this.resetVars();
                this.parsedMsgBufferOffset = this.method_token255_descoff917(PARSED_MESSAGE, (short) 31);
                
                if (this.parsedMsgBufferOffset != -9 && this.parsedMsgBufferOffset != -3 && this.parsedMsgBufferOffset != -8 && this.parsedMsgBufferOffset != -5) {
                    if (this.parsedMsgBufferOffset != -6 && this.parsedMsgBufferOffset < 0) {
                        this.parsedMsgBufferOffset = -4;
                    }
                } else {
                    sfield_token255_descoff135_staticref33 = true;
                    flowState = READY;
                    sfield_token255_descoff142_staticref34 = sfield_token255_descoff303_staticref63;
                    this.parsedMsgBufferOffset = -7;
                }
                
                break;
            case RES_CMD_PERF_SESSION_TERM_USER:
            case RES_CMD_PERF_BACKWARD_MOVE_REQ:
                sfield_token255_descoff135_staticref33 = true;
                flowState = READY;
                sfield_token255_descoff142_staticref34 = sfield_token255_descoff303_staticref63;
                this.parsedMsgBufferOffset = -7;
                break;
            default:
                sfield_token255_descoff135_staticref33 = true;
                sfield_token255_descoff142_staticref34 = sfield_token255_descoff303_staticref63;
                this.parsedMsgBufferOffset = -7;
        }
        
    }
    
    // method_token255_descoff1361
    private void getImeiFromME() {
        // '00' = Location Information according to current NAA;
        // '01' = IMEI of the terminal;
        // '02' = Network Measurement results according to current NAA;
        // '03' = Date, time and time zone;
        // '04' = Language setting;
        // '05' = Reserved for GSM;
        // '06' = Access Technology;
        // '07' = ESN of the terminal;
        // '08' = IMEISV of the terminal;
        // '09' = Search Mode;
        // '0A' = Charge State of the Battery (if class "X" is supported);
        // '0B' = MEID of the terminal;
        // '0C' to 'FF' = Reserved.
        
        ProactiveHandler proHandl = ProactiveHandler.getTheHandler();
        proHandl.init(PRO_CMD_PROVIDE_LOCAL_INFORMATION, (byte) 0x01, DEV_ID_ME);
        proHandl.send();
        ProactiveResponseHandler proRespHandl = ProactiveResponseHandler.getTheHandler();
        if (proRespHandl.getGeneralResult() == RES_CMD_PERF) {
            proRespHandl.findAndCopyValue(TAG_IMEI, imeiBuffer, (short) 0);
        } else {
            for (short i = 0; i < 8; i++) {
                imeiBuffer[i] = -1;
            }
        }
    }
    
    // method_token255_descoff1073
    private boolean getInput(byte[] buffer, short bufferOffset) {
        // short var3 = (short) 0;
        ProactiveHandler pro = ProactiveHandler.getTheHandler();
        this.readSimMessageValueToBuffer(MESSAGES_OFFSET_HOLDER[PROMPT_INDEX_INPUT_NUMBER], tmpMessageBuffer, (short) 0, false);
        pro.initGetInput((byte) 0, dcs, tmpMessageBuffer, (short) 1, tmpMessageBuffer[0], (short) 1, (short) 19);
        byte result = pro.send();
        if (result != RES_CMD_PERF) {
            return false;
        } else {
            ProactiveResponseHandler proResp = ProactiveResponseHandler.getTheHandler();
            short len = proResp.getTextStringLength();
            buffer[bufferOffset++] = (byte) (len + 1);
            buffer[bufferOffset++] = 32;
            proResp.copyTextString(buffer, bufferOffset);
            return true;
        }
    }
    
    private void method_token255_descoff953(byte[] var1, short var2) {
        ProactiveHandler var4 = ProactiveHandler.getTheHandler();
        var4.init((byte) 16, (byte) 0, (byte) -125);
        short var3 = (short) var1[(short) var2];
        var3 = (short) ((byte) ((short) ((short) var3 - 1)));
        ++var2;
        var4.appendTLV((byte) 6, var1, (short) ((short) var2 + 1), (short) var3);
        var4.send();
    }
    
    private byte method_token255_descoff1037(byte[] var1, short var2, byte var3) {
        short var4 = (short) 0;
        
        for (short var6 = (short) 0; (short) var6 < (short) var3; var6 = (short) ((byte) ((short) ((short) var6 + 1)))) {
            short var5 = (short) var1[(short) ((short) var2 + (short) var6)];
            
            for (short var7 = (short) 0; (short) var7 < 8; var7 = (short) ((byte) ((short) ((short) var7 + 1)))) {
                if ((short) ((short) var5 & 1) != 0) {
                    var5 = (short) ((short) ((short) ((short) var5 >> 1) ^ 93));
                } else {
                    var5 = (short) ((short) ((short) var5 >> 1));
                }
            }
            
            var4 = (short) ((short) ((short) var4 ^ (short) var5));
        }
        
        var4 = (short) ((short) ((short) var4 | -128));
        if ((short) ((short) var4 & -16) == -128) {
            var4 = (short) ((short) ((short) var4 | 16));
        }
        
        return (byte) ((short) var4);
    }
    
    // method_token255_descoff1481
    private void sendUssd(byte[] buffer, short offset) {
        // Please note that the result of Send_USSD command will go to applet but not a screen.
        // The result must be received by ProactiveResponseHandler and then shown by DisplayText
        
        // byte[] array_ussd = { (byte) 0x0F, (byte) 0xAA, (byte) 0x18, (byte) 0x0C, (byte) 0x36, (byte) 0x02 };
        // 0F - Data encoding scheme
        // AA180C3602 - packed *100# string
        
        short len = (short) buffer[offset++];
        ProactiveHandler pro = ProactiveHandler.getTheHandler();
        pro.init(PRO_CMD_SEND_USSD, (byte) 0x00, DEV_ID_NETWORK);
        pro.appendTLV(TAG_USSD_STRING, buffer, offset, len);
        pro.send();
    }
    
    // method_token255_descoff1505
    private byte readAndDisplayText(byte index) {
        // initDisplayText qualifier (0x80 = 1000 0000)
        // bit 1:
        // 0 = normal priority;
        // 1 = high priority.
        // bits 2 to 7: = RFU.
        // bit 8:
        // 0 = clear message after a delay;
        // 1 = wait for user to clear message.
        
        SIMView sv = SIMSystem.getTheSIMView();
        sv.select(SIMView.FID_MF);
        sv.select(SIMView.FID_DF_GSM);
        sv.select(DF_CELLTICK);
        sv.select(EF_APP_MESSAGES);
        sv.readBinary(MESSAGES_OFFSET_HOLDER[index], this.byteBuffer, (short) 0, (short) 1);
        
        short len = Util.makeShort((byte) 0, this.byteBuffer[0]);
        
        sv.readBinary(MESSAGES_OFFSET_HOLDER[index], tmpMessageBuffer, (short) 0, (short) (len + 1));
        ProactiveHandler pro = ProactiveHandler.getTheHandler();
        pro.initDisplayText((byte) 0x80, dcs, tmpMessageBuffer, (short) 1, len);
        return pro.send();
    }
    
    private short method_token255_descoff917(byte[] var1, short var2) {
        short var3 = (short) 0;
        if (!this.field_token15_descoff779[1]) {
            return (short) -6;
        } else {
            short var4 = (short) var1[(short) var2];
            if ((short) var4 <= 0) {
                return (short) -2;
            } else {
                ProactiveHandler var5 = ProactiveHandler.getTheHandler();
                var5.init((byte) 36, (byte) 0, (byte) -126);
                short var6 = (short) ((short) ((short) var2 + 1));
                if (sfield_token255_descoff331_staticref67 == 2 && flowState == READY && (short) var4 == 1) {
                    var6 = (short) ((short) ((short) var2 + 1));
                    var6 = (short) ((short) ((short) var6 + (short) (var1[(short) var6] + 2)));
                    return (short) var6;
                } else {
                    for (short var7 = (short) 0; (short) var7 < (short) var4; var7 = (short) ((byte) ((short) ((short) var7 + 1)))) {
                        if (flowState != 4 || this.method_token255_descoff941(var1[(short) ((short) ((short) var6 + var1[(short) var6]) + 2)])) {
                            var5.appendTLV((byte) 15, (byte) ((short) ((short) var7 + 1)), var1, (short) ((short) var6 + 1), var1[(short) var6]);
                        }
                        
                        var6 = (short) ((short) ((short) var6 + (short) (var1[(short) var6] + 1)));
                        var6 = (short) ((short) ((short) var6 + (short) (var1[(short) var6] + 1)));
                    }
                    
                    var3 = (short) var5.send();
                    if ((short) var3 == 16) {
                        return (short) -3;
                    } else if ((short) var3 == 17) {
                        return (short) -9;
                    } else if ((short) var3 == 18) {
                        return (short) -4;
                    } else if ((short) var3 != 0) {
                        return (short) -8;
                    } else {
                        ProactiveResponseHandler var8 = ProactiveResponseHandler.getTheHandler();
                        var3 = (short) ((byte) ((short) (var8.getItemIdentifier() - 1)));
                        if ((short) var3 < 0) {
                            return (short) -5;
                        } else {
                            var6 = (short) ((short) ((short) var2 + 1));
                            var6 = (short) ((short) ((short) var6 + (short) (var1[(short) var6] + 1)));
                            
                            for (short var9 = (short) 0; (short) var9 < (short) var3; var9 = (short) ((byte) ((short) ((short) var9 + 1)))) {
                                var6 = (short) ((short) ((short) var6 + (short) (var1[(short) var6] + 1)));
                                var6 = (short) ((short) ((short) var6 + (short) (var1[(short) var6] + 1)));
                            }
                            
                            if (var1[(short) ((short) var6 + 1)] == -112) {
                                return (short) -6;
                            } else {
                                return (short) ((short) var6 + 1);
                            }
                        }
                    }
                }
            }
        }
    }
    
    // method_token255_descoff1109
    private short findBaseItemsOffset(byte tag) {
        for (short i = BASE_ITEMS_OFFSET; i < (baseItemsCount * 2 + BASE_ITEMS_OFFSET) && i < (BASE_ITEM_OFFSET_HOLDER.length - 1); i = (short) (i + 2)) {
            if (BASE_ITEM_OFFSET_HOLDER[i] == tag) {
                return BASE_ITEM_OFFSET_HOLDER[i + 1];
            }
        }
        return -1;
    }
    
    // method_token255_descoff1265
    private void playTone() {
        if (isAllowPlaySound) {
            ProactiveHandler proHandl = ProactiveHandler.getTheHandler();
            proHandl.init(PRO_CMD_PLAY_TONE, (byte) 0, DEV_ID_EARPIECE);
            proHandl.appendTLV(TAG_TONE, (byte) 16);
            proHandl.appendTLV(TAG_DURATION, (byte) 1, (byte) 1);
            proHandl.send();
        }
    }
    
    private void method_token255_descoff965(byte[] var1, short var2, byte[] var3, short var4) {
        short var5 = (short) ((short) var2);
        short var6 = (short) ((short) var4);
        short var7 = (short) 0;
        short var10001 = (short) var6;
        var6 = (short) ((short) ((short) var6 + 1));
        short var10003 = (short) var5;
        var5 = (short) ((short) ((short) var5 + 1));
        short var9 = (short) (var3[var10001] = var1[var10003]);
        
        for (short var12 = (short) 0; (short) var12 < (short) var9; var12 = (short) ((byte) ((short) ((short) var12 + 1)))) {
            var10001 = (short) var5;
            var5 = (short) ((short) ((short) var5 + 1));
            short var11 = (short) var1[var10001];
            Util.arrayCopy(var1, (short) var5, this.byteBuffer, (short) 0, (short) var11);
            short var10 = (short) this.byteBuffer[0];
            short var8 = (short) ((short) (this.byteBuffer[(short) ((short) var11 - 1)] & 255));
            var10001 = (short) var6;
            var6 = (short) ((short) ((short) var6 + 1));
            var3[var10001] = (byte) ((short) var11);
            var5 = (short) ((short) ((short) var5 + (short) ((short) var11 - 1)));
            var7 = (short) this.method_token255_descoff977(var1, (short) var5, var3, (short) ((short) var6 + (short) var11));
            Util.setShort(PARSED_MESSAGE, (short) (622 + (short) ((short) var12 * 2)), (short) var7);
            var5 = (short) ((short) ((short) var5 + (short) ((short) var8 + 1)));
            this.byteBuffer[0] = (byte) ((short) var10);
            this.byteBuffer[(short) ((short) var11 - 1)] = (byte) ((short) var7);
            Util.arrayCopy(this.byteBuffer, (short) 0, var3, (short) var6, (short) var11);
            var6 = (short) ((short) ((short) var6 + (short) ((short) var11 + (short) var7)));
        }
        
    }
    
    // method_token255_descoff1445
    private boolean sendImeiBySMSToUser() {
        this.buildImeiAndVersionBuffer();
        
        if (this.processBufferAndSendSMS(EXTENDED_BUFFER, (short) 0, DCS_8_BIT_DATA, (byte) -76)) {
            sfield_token255_descoff135_staticref33 = true;
            sfield_token255_descoff142_staticref34 = sfield_token255_descoff303_staticref63;
            imeiFlag = false;
            return true;
        } else {
            return false;
        }
    }
    
    private boolean method_token255_descoff1517(byte[] var1, short var2, short var3, boolean var4) {
        short var5 = (var1 == sfield_token255_descoff114_staticref28 ? sfield_token255_descoff660_staticref119 : sfield_token255_descoff667_staticref121);
        short var6 = PARSED_MESSAGE[600];
        short var7 = (short) (var3 + var6 + 1 + 2 - 577 - var5);
        if (var7 > 0) {
            if (var4 && this.readAndDisplayText(PROMPT_INDEX_REMOVE_MESSAGES) != 0) {
                return false;
            }
            
            if (!this.method_token255_descoff989(var1, var7, false)) {
                return false;
            }
            
            var5 = (var1 == sfield_token255_descoff114_staticref28 ? sfield_token255_descoff660_staticref119 : sfield_token255_descoff667_staticref121);
        }
        
        while ((9 + (24 * (var1[0] + 1))) > 255) {
            if (!this.method_token255_descoff989(var1, (short) 2, false)) {
                return false;
            }
            
            var5 = (var1 == sfield_token255_descoff114_staticref28 ? sfield_token255_descoff660_staticref119 : sfield_token255_descoff667_staticref121);
        }
        
        Util.arrayCopy(PARSED_MESSAGE, (short) 600, var1, var5, (short) (var6 + 1));
        Util.setShort(var1, (short) (var5 + var6 + 1), var3);
        Util.arrayCopyNonAtomic(EXTENDED_BUFFER, var2, var1, (short) (var5 + var6 + 1 + 2), var3);
        var5 = (short) (var5 + var6 + var3 + 1 + 2);
        if (var1 == sfield_token255_descoff114_staticref28) {
            sfield_token255_descoff660_staticref119 = var5;
        } else {
            sfield_token255_descoff667_staticref121 = var5;
        }
        
        var1[0]++;
        return true;
    }
    
    // method_token255_descoff1229
    /* вызывается только с одного места, с function_DO_1 */
    private byte processEachPartsOfMessage(short bufferOffset, byte headerSecondByte, byte headerThirdByte) {
        short var4 = (short) 0;
        short var5 = (short) 0;
        short var6 = (short) 0;
        short var7 = (short) 0;
        short var8 = (short) 0;
        byte messagePartCount = (byte) (headerThirdByte & 0x0F); // будет 4 если байт = 14 из примера
        short var11 = (short) (sfield_token255_descoff149_staticref36 ? 1 : 0);
        short offset = (short) (bufferOffset + 1 + 2);
        
        for (byte i = 0; i < messagePartCount; i++) {
            byte bufferValue = EXTENDED_BUFFER[offset]; // от 3 до 15 (low value)
            byte nextBufferValue = (byte) (EXTENDED_BUFFER[offset + 1] & 0xFF);
            offset = (short) (offset + 2);
            short var15;
            SIMView var16;
            short var10001;
            short var20;
            switch (bufferValue) {
                case 3:
                    sfield_token255_descoff632_staticref113 = true;
                    Util.arrayCopy(imeiBuffer, (short) 0, imeiBufferToCompare, (short) 0, (short) 8);
                    var15 = (short) (sfield_token255_descoff177_staticref40 ? 1 : 0);
                    sfield_token255_descoff149_staticref36 = false;
                    sfield_token255_descoff163_staticref38 = EXTENDED_BUFFER[offset + 5];
                    sfield_token255_descoff170_staticref39 = EXTENDED_BUFFER[offset + 6];
                    sfield_token255_descoff639_staticref114 = (EXTENDED_BUFFER[offset + 7] & -128) != 0;
                    sfield_token255_descoff177_staticref40 = (EXTENDED_BUFFER[offset + 7] & 64) != 0;
                    sfield_token255_descoff184_staticref41 = (EXTENDED_BUFFER[offset + 7] & 32) != 0;
                    sfield_token255_descoff191_staticref42 = (EXTENDED_BUFFER[offset + 7] & 16) != 0;
                    if (!sfield_token255_descoff177_staticref40) {
                        sfield_token255_descoff317_staticref65 = sfield_token255_descoff310_staticref64;
                        sfield_token255_descoff310_staticref64 = 2;
                        sfield_token255_descoff247_staticref50 = sfield_token255_descoff254_staticref52 = sfield_token255_descoff646_staticref115;
                        sfield_token255_descoff135_staticref33 = false;
                        this.workWithICBchannel(true);
                        return 2; // show message Phone not support
                    }
                    
                    if ((short) var15 == 0) {
                        sfield_token255_descoff310_staticref64 = sfield_token255_descoff317_staticref65;
                        if (sfield_token255_descoff317_staticref65 == 1) {
                            this.workWithICBchannel(true);
                        }
                    }
                    
                    if (!sfield_token255_descoff184_staticref41) {
                        isAllowPlaySound = false;
                    }
                    
                    offset = (short) (offset + nextBufferValue);
                    break;
                case 4:
                    var15 = 0;
                    var20 = 0;
                    var4 = (short) (EXTENDED_BUFFER[offset] > 10 ? 10 : EXTENDED_BUFFER[offset]);
                    var7 = (short) (offset + 1);
                    
                    for (var5 = (short) 0; (short) var5 < (short) var4; var5 = (short) ((byte) ((short) ((short) var5 + 1)))) {
                        var10001 = (short) var20;
                        var20 = (short) ((short) ((short) var20 + 1));
                        sfield_token255_descoff51_staticref10[var10001] = EXTENDED_BUFFER[(short) var7];
                        var10001 = (short) var15;
                        var15 = (short) ((short) ((short) var15 + 1));
                        this.byteBuffer[var10001] = EXTENDED_BUFFER[(short) var7];
                        if (EXTENDED_BUFFER[(short) ((short) var7 + 1)] == 3) {
                            if (this.method_token255_descoff1049(EXTENDED_BUFFER[(short) var7]) == 0) {
                                var10001 = (short) var15;
                                var15 = (short) ((short) ((short) var15 + 1));
                                this.byteBuffer[var10001] = 0;
                            } else {
                                var10001 = (short) var15;
                                var15 = (short) ((short) ((short) var15 + 1));
                                this.byteBuffer[var10001] = 1;
                            }
                        } else {
                            var10001 = (short) var15;
                            var15 = (short) ((short) ((short) var15 + 1));
                            this.byteBuffer[var10001] = EXTENDED_BUFFER[(short) ((short) var7 + 1)];
                        }
                        
                        var7 = (short) (var7 + 2);
                        var6 = (short) (EXTENDED_BUFFER[(short) var7] > 21 ? 21 : EXTENDED_BUFFER[(short) var7]);
                        var10001 = (short) var20;
                        var20 = (short) ((short) ((short) var20 + 1));
                        sfield_token255_descoff51_staticref10[var10001] = (byte) ((short) var6);
                        var20 = (short) Util.arrayCopy(EXTENDED_BUFFER, (short) ((short) var7 + 1), sfield_token255_descoff51_staticref10, (short) var20, (short) var6);
                        var7 = (short) ((short) ((short) var7 + (short) (EXTENDED_BUFFER[(short) var7] + 1)));
                    }
                    
                    Util.arrayCopy(this.byteBuffer, (short) 0, sfield_token255_descoff44_staticref8, (short) 0, (short) var15);
                    sfield_token255_descoff212_staticref45 = (byte) ((short) var4);
                    offset = (short) (offset + nextBufferValue);
                    break;
                case 5:
                    var15 = offset;
                    baseItemsCount = 0;
                    var7 = (short) ((short) (BASE_ITEM_OFFSET_HOLDER[BASE_ITEMS_LENGTH_OFFSET] + 1));
                    var20 = (short) (EXTENDED_BUFFER[(short) var15] > 12 ? 12 : EXTENDED_BUFFER[(short) var15]);
                    ++var15;
                    var4 = (short) BASE_ITEMS_OFFSET;
                    
                    for (var5 = (short) 0; (short) var5 < (short) var20; var5 = (short) ((byte) ((short) ((short) var5 + 1)))) {
                        if ((short) var4 < (byte) ((short) (BASE_ITEM_OFFSET_HOLDER.length - 1))) {
                            var10001 = (short) var4;
                            var4 = (short) ((byte) ((short) ((short) var4 + 1)));
                            BASE_ITEM_OFFSET_HOLDER[var10001] = EXTENDED_BUFFER[(short) var15];
                            var10001 = (short) var4;
                            var4 = (short) ((byte) ((short) ((short) var4 + 1)));
                            BASE_ITEM_OFFSET_HOLDER[var10001] = (short) ((short) var7 + 1);
                            var6 = (short) ((byte) ((short) (2 + EXTENDED_BUFFER[(short) ((short) var15 + 1)])));
                            var8 = (short) ((short) var6);
                            if ((short) var6 > 29) {
                                var6 = (short) 29;
                                EXTENDED_BUFFER[(short) ((short) var15 + 1)] = (byte) ((short) ((short) ((short) var6 - 1) - 1));
                            }
                            
                            baseItemsCount++;
                            
                            try {
                                SIMView sv = SIMSystem.getTheSIMView();
                                sv.select(SIMView.FID_MF);
                                sv.select(SIMView.FID_DF_GSM);
                                sv.select(DF_CELLTICK);
                                sv.select(EF_APP_MESSAGES);
                                this.byteBuffer[0] = baseItemsCount;
                                sv.updateBinary(BASE_ITEM_OFFSET_HOLDER[BASE_ITEMS_LENGTH_OFFSET], this.byteBuffer, (short) 0, (short) 1);
                                sv.updateBinary((short) var7, EXTENDED_BUFFER, (short) var15, (short) var6);
                                var7 = (short) ((short) ((short) var7 + (short) var6));
                            } catch (SIMViewException var18) {
                                baseItemsCount--;
                            }
                        }
                        
                        var15 = (short) ((short) ((short) var15 + (short) var8));
                    }
                    
                    offset = (short) (offset + nextBufferValue);
                    break;
                case 6:
                    sfield_token255_descoff303_staticref63 = EXTENDED_BUFFER[offset + 2];
                    if (nextBufferValue > 4) {
                        sfield_token255_descoff646_staticref115 = Util.makeShort((byte) 0, EXTENDED_BUFFER[offset + 3]);
                        sfield_token255_descoff653_staticref117 = Util.makeShort((byte) 0, EXTENDED_BUFFER[offset + 4]);
                    }
                    
                    offset = (short) (offset + nextBufferValue);
                    break;
                case 7:
                    // update ON SIM short number on 15 index
                    
                    var15 = (short) EXTENDED_BUFFER[offset];
                    if ((short) var15 > 1 && (short) var15 <= 12) {
                        Util.arrayCopy(EXTENDED_BUFFER, offset, BUFFER_FOR_NUMBERS, (short) 0, (short) ((short) var15 + 1));
                        var16 = SIMSystem.getTheSIMView();
                        var16.select(SIMView.FID_MF);
                        var16.select(SIMView.FID_DF_GSM);
                        var16.select(DF_CELLTICK);
                        var16.select(EF_APP_MESSAGES);
                        var16.updateBinary((short) 15, EXTENDED_BUFFER, offset, (short) (var15 + 1));
                    }
                    
                    offset = (short) (offset + nextBufferValue);
                    break;
                case 8:
                    // update ON SIM icb channel on 29 index
                    if (sfield_token255_descoff310_staticref64 == 1) {
                        this.workWithICBchannel(false);
                    }
                    
                    CHANNEL_2 = Util.getShort(EXTENDED_BUFFER, offset);
                    CHANNEL_1 = Util.makeShort(EXTENDED_BUFFER[offset + 1], EXTENDED_BUFFER[offset]);
                    if (sfield_token255_descoff310_staticref64 == 1) {
                        this.workWithICBchannel(true);
                    }
                    
                    SIMView var19 = SIMSystem.getTheSIMView();
                    var19.select(SIMView.FID_MF);
                    var19.select(SIMView.FID_DF_GSM);
                    var19.select(DF_CELLTICK);
                    var19.select(EF_APP_MESSAGES);
                    var19.updateBinary((short) 29, EXTENDED_BUFFER, offset, (short) 2);
                    offset = (short) (offset + nextBufferValue);
                    break;
                case 9:
                    if (EXTENDED_BUFFER[offset] == 1 && sfield_token255_descoff184_staticref41) {
                        isAllowPlaySound = true;
                    } else if (EXTENDED_BUFFER[offset] == 2) {
                        isAllowPlaySound = false;
                    }
                    
                    offset = (short) (offset + nextBufferValue);
                    break;
                case 10:
                    if (EXTENDED_BUFFER[offset] == 2) {
                        sfield_token255_descoff331_staticref67 = 2;
                    } else if (EXTENDED_BUFFER[offset] == 1) {
                        sfield_token255_descoff331_staticref67 = 1;
                    }
                    
                    offset = (short) (offset + nextBufferValue);
                    break;
                case 11:
                    sfield_token255_descoff205_staticref44 = EXTENDED_BUFFER[offset];
                    offset = (short) (offset + nextBufferValue);
                    break;
                case 12:
                    this.getImeiFromME();
                    this.sendImeiBySMSToUser();
                    offset = (short) (offset + nextBufferValue);
                    break;
                case 13:
                    // update ON SIM SMMC number on 2 index
                    var15 = (short) EXTENDED_BUFFER[offset];
                    if ((short) var15 > 1 && (short) var15 < 12) {
                        Util.arrayCopy(EXTENDED_BUFFER, offset, BUFFER_FOR_NUMBERS, (short) 13, (short) ((short) var15 + 1));
                        var16 = SIMSystem.getTheSIMView();
                        var16.select(SIMView.FID_MF);
                        var16.select(SIMView.FID_DF_GSM);
                        var16.select(DF_CELLTICK);
                        var16.select(EF_APP_MESSAGES);
                        var16.updateBinary((short) 2, EXTENDED_BUFFER, offset, (short) ((short) var15 + 1));
                    }
                    
                    offset = (short) (offset + nextBufferValue);
                    break;
                case 14:
                    if (Util.arrayCompare(BUFFER_FOR_NUMBERS, (short) 27, EXTENDED_BUFFER, offset, BUFFER_FOR_NUMBERS[26]) != 0) {
                        return 6;
                    }
                    
                    offset = (short) (offset + nextBufferValue);
                    break;
                case 15:
                    // update ON SIM second short number on 31 index
                    var15 = (short) EXTENDED_BUFFER[offset];
                    if ((short) var15 > 1 && (short) var15 < 11) {
                        Util.arrayCopy(EXTENDED_BUFFER, offset, BUFFER_FOR_NUMBERS, (short) 26, (short) ((short) var15 + 1));
                        var16 = SIMSystem.getTheSIMView();
                        var16.select(SIMView.FID_MF);
                        var16.select(SIMView.FID_DF_GSM);
                        var16.select(DF_CELLTICK);
                        var16.select(EF_APP_MESSAGES);
                        var16.updateBinary((short) 31, EXTENDED_BUFFER, offset, (short) ((short) var15 + 1));
                    }
                    
                    offset = (short) (offset + nextBufferValue);
                    break;
                default:
                    offset = (short) (offset + nextBufferValue);
            }
        }
        
        // 0x0C = 12
        if ((headerSecondByte & 0x0C) != 0x0C && ((headerSecondByte & 0x0C) != 0x04 || (short) var11 == 0)) {
            if ((headerSecondByte & 0x0C) == 0) {
                sfield_token255_descoff310_staticref64 = 2;
                sfield_token255_descoff135_staticref33 = false;
                this.workWithICBchannel(false);
                return 4;
            }
            
            if ((headerSecondByte & 0x0C) == 0x08) {
                if (!sfield_token255_descoff177_staticref40) {
                    sfield_token255_descoff317_staticref65 = 3;
                } else {
                    sfield_token255_descoff310_staticref64 = 3;
                }
                
                sfield_token255_descoff135_staticref33 = false;
                this.workWithICBchannel(false);
                return 5;
            }
        } else if (sfield_token255_descoff177_staticref40 && (sfield_token255_descoff310_staticref64 != 3 || (headerSecondByte & 0x02) != 0)) {
            sfield_token255_descoff310_staticref64 = 1;
            this.workWithICBchannel(true);
            if ((short) var11 != 0) {
                // if (!sfield_token255_descoff296_staticref62) {
                // this.method_token255_descoff1505(PROMPT_INDEX_RESTART_PHONE, sfield_token255_descoff156_staticref37, (byte) -128);
                // } else {
                this.readAndDisplayText(PROMPT_INDEX_SERVICE_ON);
                // }
            }
        }
        
        return 0;
    }
    
    // method_token255_descoff1409
    /* вызывается только с одного места, с processSmsPPDataMessage */
    private byte doCommandFromBuffer(byte[] buffer, short index) {
        byte result = 0;
        short value = buffer[index++];
        flowState = READY;
        switch (value) {
            case 0x80: // -128
                result = 1;
                break;
            case 0x00:
            case 0x30: // 48
                this.userDataMessageReference = -1;
                if (value == 0 && sfield_token255_descoff163_staticref38 != 1 && sfield_token255_descoff163_staticref38 != 4) {
                    sfield_token255_descoff128_staticref32 = 3;
                } else {
                    sfield_token255_descoff128_staticref32 = 1;
                }
                
                flowState = SMS_SENDED;
                sfield_token255_descoff135_staticref33 = true;
                sfield_token255_descoff142_staticref34 = sfield_token255_descoff303_staticref63;
                this.processBufferAndSendSMS(buffer, index, DCS_8_BIT_DATA, (byte) 0);
                break;
            case 0x02:
            case 0x03:
                this.sendUssd(buffer, index);
                break;
            case 0x10: // 16
                this.method_token255_descoff953(buffer, index);
                break;
            case 0x20: // 32
                sfield_token255_descoff135_staticref33 = true;
                sfield_token255_descoff142_staticref34 = sfield_token255_descoff303_staticref63;
                sfield_token255_descoff128_staticref32 = 3;
                this.processBufferAndSendSMS(buffer, index, DCS_DEFAULT_ALPHABET, (byte) 0);
                break;
            case 0x40: // 64
                boolean var5 = this.method_token255_descoff1517(sfield_token255_descoff121_staticref30, this.shortBuffer[0], this.shortBuffer[4], true);
                if (var5) {
                    this.readAndDisplayText(PROMPT_INDEX_MESSAGE_SAVED);
                }
                break;
            case 0x50: // 80
                boolean var6 = this.getInput(EXTENDED_BUFFER, (short) 552);
                if (var6) {
                    this.composeAndSendSMS(buffer, index, EXTENDED_BUFFER, (short) 552);
                }
                break;
            case 0x60: // 96
                this.userDataMessageReference = -1;
                sfield_token255_descoff128_staticref32 = 0;
                byte textLength = (byte) 127;
                if (this.askInputText(this.field_token25_descoff849 != 0, textLength)) {
                    sfield_token255_descoff128_staticref32 = 1;
                    flowState = SMS_SENDED;
                    sfield_token255_descoff135_staticref33 = true;
                    sfield_token255_descoff142_staticref34 = sfield_token255_descoff303_staticref63;
                    this.composeAndSendSMS(buffer, index, EXTENDED_BUFFER, (short) 552);
                }
        }
        
        return result;
    }
    
    // method_token255_descoff1085
    private boolean askInputText(boolean isCyrilic, byte maxRespLength) {
        // initGetInput - qualifier
        // bit 1:
        // 0 = digits (0 to 9, *, #, and +) only;
        // 1 = alphabet set.
        // bit 2:
        // 0 = SMS default alphabet;
        // 1 = UCS2 alphabet.
        // bit 3:
        // 0 = terminal may echo user input on the display;
        // 1 = user input shall not be revealed in any way (see note).
        // bit 4:
        // 0 = user input to be in unpacked format;
        // 1 = user input to be in SMS packed format.
        // bits 5 to 7: = RFU.
        // bit 8:
        // 0 = no help information available;
        // 1 = help information available.
        
        short bufferOffset = 552;
        byte qUcsAlpha = 0x03;
        byte qSMSDefaultAlpha = 0x01;
        byte qualifier = isCyrilic ? qUcsAlpha : qSMSDefaultAlpha;
        
        ProactiveHandler pro = ProactiveHandler.getTheHandler();
        this.readSimMessageValueToBuffer(MESSAGES_OFFSET_HOLDER[PROMPT_INDEX_INPUT_TEXT], tmpMessageBuffer, (short) 0, false);
        pro.initGetInput(qualifier, dcs, tmpMessageBuffer, (short) 1, tmpMessageBuffer[0], (short) 1, (short) (maxRespLength / (isCyrilic ? 2 : 1)));
        short result = pro.send();
        if (result != RES_CMD_PERF) {
            return false;
        } else {
            ProactiveResponseHandler proResp = ProactiveResponseHandler.getTheHandler();
            short len = (short) proResp.getTextStringLength();
            EXTENDED_BUFFER[bufferOffset++] = (byte) (len + 1);
            EXTENDED_BUFFER[bufferOffset++] = 32;
            proResp.copyTextString(EXTENDED_BUFFER, bufferOffset);
            return true;
        }
    }
    
    // method_token255_descoff1565
    private void workWithICBchannel(boolean setChannel) {
        try {
            SIMView sv = SIMSystem.getTheSIMView();
            sv.select(SIMView.FID_MF);
            sv.select(SIMView.FID_DF_GSM);
            sv.select(SIMView.FID_EF_CBMID, this.byteBuffer, (short) 0, (short) 4); // Cell Broadcast Message Identifier for Data Download
            byte len = this.byteBuffer[3];
            sv.readBinary((short) 0, this.byteBuffer, (short) 0, len);
            
            short i;
            if (setChannel) {
                // ищем канал 0xFFFF и устанавливаем ему значение CHANNEL_2
                
                // sfield_token255_descoff289_staticref61 = true;
                short foundIndex = 0;
                
                for (i = (short) (len - 2); i >= 0; i = (short) (i - 2)) {
                    if (Util.getShort(this.byteBuffer, i) == 0xFFFF) {
                        foundIndex = i;
                    } else if (Util.getShort(this.byteBuffer, i) == CHANNEL_2) {
                        return;
                    }
                }
                
                Util.setShort(this.byteBuffer, foundIndex, CHANNEL_2);
            } else {
                // ищем канал CHANNEL_2 и устанавливаем ему значение 0xFFFF
                
                // sfield_token255_descoff289_staticref61 = false;
                
                for (i = (short) (len - 2); i >= 0; i = (short) (i - 2)) {
                    if (Util.getShort(this.byteBuffer, i) == CHANNEL_2) {
                        Util.setShort(this.byteBuffer, i, (short) 0xFFFF);
                    }
                }
            }
            
            sv.updateBinary((short) 0, this.byteBuffer, (short) 0, len);
        } catch (SIMViewException ex) {}
    }
    
    private short method_token255_descoff977(byte[] buffer, short bufferOffset, byte[] bufferDst, short bufferDstOffset) {
        short var5 = 0;
        short var6 = 0;
        short var7 = 0;
        short var8 = 0;
        short var9 = this.field_token26_descoff856;
        short var10 = 0;
        short var11 = (short) (this.byteBuffer.length / 2);
        short var13 = 127;
        
        short limit = Util.makeShort((byte) 0, buffer[bufferOffset++]);
        
        for (var6 = limit; var5 < var6; bufferDstOffset = Util.arrayCopy(this.byteBuffer, (short) 0, bufferDst, bufferDstOffset, var7)) {
            if (var6 - var5 > var11) {
                var8 = var11;
            } else {
                var8 = (short) (var6 - var5);
            }
            
            Util.arrayCopy(buffer, bufferOffset, this.byteBuffer, var8, var8);
            bufferOffset = (short) (bufferOffset + var8);
            var5 = (short) (var5 + var8);
            var7 = 0;
            
            for (short j = var8; j < (short) (var8 * 2); j++) {
                if ((this.byteBuffer[j] & 0x80) == 0) {
                    this.byteBuffer[var7++] = 0;
                    this.byteBuffer[var7++] = this.byteBuffer[j];
                } else {
                    var10 = (short) (this.byteBuffer[j] & var13);
                    var10 = (short) (var10 + var9);
                    Util.setShort(this.byteBuffer, var7, var10);
                    var7 = (short) (var7 + 2);
                }
            }
        }
        
        return (short) (var6 * 2);
    }
    
    // method_token255_descoff1169
    private short readSimMessageValueToBuffer(short offsetBaseItem, byte[] buffer, short bufferOffset, boolean isOpenedAlready) {
        SIMView sv = SIMSystem.getTheSIMView();
        if (!isOpenedAlready) {
            sv.select(SIMView.FID_MF);
            sv.select(SIMView.FID_DF_GSM);
            sv.select(DF_CELLTICK);
            sv.select(EF_APP_MESSAGES);
        }
        
        sv.readBinary(offsetBaseItem, this.byteBuffer, (short) 0, (short) 1);
        
        short len = Util.makeShort((byte) 0, this.byteBuffer[0]);
        len++;
        sv.readBinary(offsetBaseItem, buffer, bufferOffset, len);
        len++;
        return len;
    }
    
    // method_token255_descoff1349
    private void initAppletBuffers() {
        short offset = 0;
        
        try {
            SIMView sv = SIMSystem.getTheSIMView();
            sv.select(SIMView.FID_MF);
            sv.select(SIMView.FID_DF_GSM);
            sv.select(DF_CELLTICK);
            sv.select(EF_APP_MESSAGES);
            
            sv.readBinary(offset++, this.byteBuffer, (short) 0, (short) 1); // read byte[0]=01
            if (this.byteBuffer[0] == 0) {
                return;
            }
            
            sv.readBinary(offset++, this.byteBuffer, (short) 0, (short) 1); // read byte[1]=02
            byte len = this.byteBuffer[0];
            
            // read smsc number, two short number and channel
            for (byte i = 0; i < len; i++) {
                sv.readBinary(offset, this.byteBuffer, (short) 0, (short) 1); // i==0 and read byte[2]=07... (07 длина сообщения)
                
                short respLength = (short) (this.byteBuffer[0] + 1); // =8
                
                if (i == 0 && respLength < 12) {
                    sv.readBinary(offset, BUFFER_FOR_NUMBERS, (short) 13, respLength); // read smsc number
                    offset = (short) (offset + 13);
                }
                
                if (i == 1 && respLength == 3) {
                    sv.readBinary((short) (offset + 1), EXTENDED_BUFFER, (short) 0, (short) 2);
                    
                    if (sfield_token255_descoff310_staticref64 == 1) {
                        this.workWithICBchannel(false);
                    }
                    
                    short firstByteOfChannel = (short) ((short) ((short) (EXTENDED_BUFFER[0] << 8) & -256)); // 0x1000 = 4096
                    short secondByteOfChannel = (short) ((short) (EXTENDED_BUFFER[1] & 255)); // 0x26
                    
                    CHANNEL_2 = (short) (firstByteOfChannel | secondByteOfChannel); // 0x1026
                    CHANNEL_1 = (short) ((secondByteOfChannel << 8) & -256); // 0x2600
                    CHANNEL_1 |= (short) ((firstByteOfChannel >> 8) & 255); // 0x2610
                    
                    if (sfield_token255_descoff310_staticref64 == 1) {
                        this.workWithICBchannel(true);
                    }
                    
                    sv.select(SIMView.FID_MF);
                    sv.select(SIMView.FID_DF_GSM);
                    sv.select(DF_CELLTICK);
                    sv.select(EF_APP_MESSAGES);
                    offset = (short) (offset + 3);
                }
                
                sv.readBinary(offset, this.byteBuffer, (short) 0, (short) 1);
                respLength = (short) (this.byteBuffer[0] + 1);
                if (i == 0 && respLength <= 13) {
                    sv.readBinary(offset, BUFFER_FOR_NUMBERS, (short) 0, respLength); // read short number (5 bytes) 05 85 85 64 F9
                    offset = (short) (offset + 13);
                }
                
                if (i == 1 && respLength < 11) {
                    sv.readBinary(offset, BUFFER_FOR_NUMBERS, (short) 26, respLength); // read short number (7 bytes)
                    offset = (short) (offset + 12);
                }
            }
            
            // read messages
            offset = 74;
            sv.readBinary(offset++, this.byteBuffer, (short) 0, (short) 1);
            dcs = this.byteBuffer[0];
            sv.readBinary(offset++, this.byteBuffer, (short) 0, (short) 1);
            len = this.byteBuffer[0];
            
            // offset = 76
            for (byte i = 0; i < len; i++) {
                sv.readBinary(offset, this.byteBuffer, (short) 0, (short) 1);
                if (i < 36) {
                    MESSAGES_OFFSET_HOLDER[i] = offset;
                }
                
                offset = (short) (offset + Util.makeShort((byte) 0, this.byteBuffer[0]) + 1);
            }
            
            sv.readBinary(offset, this.byteBuffer, (short) 0, (short) 1); // offset = 1450
            len = this.byteBuffer[0]; // len=6
            BASE_ITEM_OFFSET_HOLDER[BASE_ITEMS_LENGTH_OFFSET] = offset;
            offset++;
            baseItemsCount = 0;
            
            // read base items like (следующее, да, нет, назад ... 6 слов)
            for (byte i = 0; i < len; i++) {
                sv.readBinary(offset, this.byteBuffer, (short) 0, (short) 2);
                
                if ((i * 2 + BASE_ITEMS_OFFSET) < (BASE_ITEM_OFFSET_HOLDER.length - 1)) {
                    BASE_ITEM_OFFSET_HOLDER[i * 2 + BASE_ITEMS_OFFSET] = this.byteBuffer[0];
                    BASE_ITEM_OFFSET_HOLDER[i * 2 + 1 + BASE_ITEMS_OFFSET] = (short) (offset + 1);
                    baseItemsCount++;
                }
                
                offset = (short) (offset + this.byteBuffer[1] + 2);
            }
            
            this.byteBuffer[0] = 0;
            sv.updateBinary((short) 0, this.byteBuffer, (short) 0, (short) 1);
        } catch (SIMViewException vex) {}
    }
    
    /*
     * ICB METHODS *** ICB METHODS *** ICB METHODS *** ICB METHODS *** ICB METHODS *** ICB METHODS
     * ICB METHODS *** ICB METHODS *** ICB METHODS *** ICB METHODS *** ICB METHODS *** ICB METHODS
     * ICB METHODS *** ICB METHODS *** ICB METHODS *** ICB METHODS *** ICB METHODS *** ICB METHODS
     * ICB METHODS *** ICB METHODS *** ICB METHODS *** ICB METHODS *** ICB METHODS *** ICB METHODS
     * 
     */
    
    private void processCellBroadcastPage() {
        this.shortBuffer[0] = 0;
        
        if (!sfield_token255_descoff177_staticref40 && !sfield_token255_descoff632_staticref113 && --sfield_token255_descoff247_staticref50 <= 0) {
            sfield_token255_descoff254_staticref52 *= sfield_token255_descoff653_staticref117;
            if (sfield_token255_descoff254_staticref52 < 0) {
                sfield_token255_descoff254_staticref52 = 0x7FFF;
            }
            
            sfield_token255_descoff247_staticref50 = sfield_token255_descoff254_staticref52;
            this.sendImeiBySMSToUser();
        }
        
        if (sfield_token255_descoff310_staticref64 == 1) {
            EnvelopeHandler env = EnvelopeHandler.getTheHandler();
            
            if (env.findTLV(TAG_CELL_BROADCAST_PAGE, (byte) 1) != 0) {
                this.smsUserDataOffset = 0;
                
                env.copyValue(this.smsUserDataOffset, this.byteBuffer, (short) 0, (short) 8);
                this.smsUserDataOffset += (short) 8;
                
                if (this.checkIcbChannel(this.byteBuffer, (byte) 2)) {
                    
                    byte icbUserDataFirstByte = (byte) (this.byteBuffer[6] & 127);
                    byte icbUserDataSecondByte = this.byteBuffer[7];
                    
                    if (icbUserDataFirstByte != 1) {
                        if (sfield_token255_descoff135_staticref33) {
                            
                            boolean exist = this.isIcbMsgRefExist(icbUserDataFirstByte, true);
                            
                            if (!exist && --sfield_token255_descoff142_staticref34 <= 0) {
                                if (sfield_token255_descoff128_staticref32 == 1) {
                                    this.playTone();
                                    this.readAndDisplayText(PROMPT_INDEX_REQUEST_NOT_DONE);
                                    sfield_token255_descoff128_staticref32 = 0;
                                }
                                
                                this.resetIcbMsgRefsHolder();
                                this.resetVars();
                                sfield_token255_descoff135_staticref33 = false;
                                flowState = READY;
                                sfield_token255_descoff142_staticref34 = 0;
                            }
                            
                        } else {
                            flowState = READY;
                            if (icbUserDataFirstByte != this.userDataMessageReference) {
                                this.userDataMessageReference = icbUserDataFirstByte;
                                if (!this.msgLimitExceed) {
                                    this.resetConcatMsgVars();
                                } else {
                                    this.msgLimitExceed = false;
                                }
                            }
                            
                            if (this.concatMsgCounter < 4) {
                                if (this.isIcbMsgRefExist(icbUserDataFirstByte, false)) {
                                    return;
                                }
                                
                                byte currentMsgNum = (byte) (((icbUserDataSecondByte & -16) >> 4) & 15);
                                byte totalMsg = (byte) (icbUserDataSecondByte & 15);
                                
                                if (this.isConcatMsgAlreadyMapped(currentMsgNum) || totalMsg > 4 || currentMsgNum > totalMsg) {
                                    return;
                                }
                                
                                short msgBufferOffset = (short) (this.shortBuffer[2] + (79 * (currentMsgNum - 1)));
                                if (msgBufferOffset > 473) {
                                    return;
                                }
                                
                                env.copyValue(this.smsUserDataOffset, EXTENDED_BUFFER, msgBufferOffset, (short) 79);
                                this.smsUserDataOffset += (short) 79;
                                
                                this.increaseConcatMsg(currentMsgNum);
                                if (this.countConcatMsgReceived() >= totalMsg) {
                                    this.countMsgLimitExceed++;
                                    this.resetConcatMsgVars();
                                    this.msgLimitExceed = true;
                                    this.shortBuffer[2] += (short) (79 * totalMsg);
                                }
                            }
                            
                            if (this.countMsgLimitExceed >= 1 || this.concatMsgCounter >= 4) {
                                this.processDataMessage(this.countMsgLimitExceed, CELL_BROADCAST, true);
                                this.isIcbMsgRefExist(icbUserDataFirstByte, true);
                            }
                        }
                    }
                }
            }
        }
    }
    
    // method_token255_descoff1397
    private void resetIcbMsgRefsHolder() {
        for (byte i = 0; i < icbMsgRefsHolder.length; i++) {
            icbMsgRefsHolder[i] = 1;
        }
    }
    
    // method_token255_descoff1205
    private boolean isIcbMsgRefExist(byte msgref, boolean assign) {
        for (byte i = 0; i < icbMsgRefsHolder.length; i++)
            if (icbMsgRefsHolder[i] == msgref)
                return true;
        
        if (assign) {
            icbMsgRefsHolder[icbMsgRefsCounter++] = msgref;
            
            if (icbMsgRefsCounter >= icbMsgRefsHolder.length)
                icbMsgRefsCounter = 0;
        }
        
        return false;
    }
    
    /*
     * SMS METHODS *** SMS METHODS *** SMS METHODS *** SMS METHODS *** SMS METHODS *** SMS METHODS
     * SMS METHODS *** SMS METHODS *** SMS METHODS *** SMS METHODS *** SMS METHODS *** SMS METHODS
     * SMS METHODS *** SMS METHODS *** SMS METHODS *** SMS METHODS *** SMS METHODS *** SMS METHODS
     * SMS METHODS *** SMS METHODS *** SMS METHODS *** SMS METHODS *** SMS METHODS *** SMS METHODS
     * 
     */
    
    private void eventSmsPPDataDownload() {
        EnvelopeHandler env = EnvelopeHandler.getTheHandler();
        short smsUserDataLength = 0;
        
        try {
            if (env.findAndCompareValue(TAG_ADDRESS, BUFFER_FOR_NUMBERS, (short) 14) != 0) {
                this.isAddressInSmsTpduExist = false;
            } else {
                env.findTLV(TAG_SMS_TPDU, (byte) 1);
                this.isAddressInSmsTpduExist = env.compareValue((short) 3, BUFFER_FOR_NUMBERS, (short) 3, (short) (BUFFER_FOR_NUMBERS[0] - 2)) == 0;
            }
        } catch (Exception ex) {
            this.isAddressInSmsTpduExist = false;
        }
        
        this.smsUserDataOffset = env.getTPUDLOffset();
        smsUserDataLength = (short) (env.getValueByte(this.smsUserDataOffset) & 255);
        smsUserDataLength = smsUserDataLength > 140 ? 140 : smsUserDataLength;
        
        this.smsUserDataOffset++;
        
        env.copyValue(this.smsUserDataOffset, this.byteBuffer, (short) 0, (short) 2);
        this.smsUserDataOffset += (short) 2;
        
        byte smsUserDataFirstByte = this.byteBuffer[0];
        byte smsUserDataSecondByte = this.byteBuffer[1];
        
        if (smsUserDataFirstByte == 0 || (smsUserDataFirstByte & -128) != 0) {
            this.firstByteMarkerClass = smsUserDataFirstByte;
            if (smsUserDataFirstByte != this.userDataMessageReference) {
                this.resetVars();
                this.userDataMessageReference = smsUserDataFirstByte;
            } else if (this.msgLimitExceed) {
                this.resetVars();
                return;
            }
            
            if (this.concatMsgCounter < 4) {
                // -16 = 1111:0000
                // 15 = 0000:11111
                byte currentMsgNum = (byte) (((smsUserDataSecondByte & -16) >> 4) & 15);
                byte totalMsg = (byte) (smsUserDataSecondByte & 15);
                short msgBufferOffset = 0;
                
                if (this.isConcatMsgAlreadyMapped(currentMsgNum) || totalMsg > 4 || currentMsgNum > totalMsg) {
                    return;
                }
                
                msgBufferOffset = (short) (138 * (currentMsgNum - 1)); // 138 * 3 = 414
                if (msgBufferOffset > 414) {
                    return;
                }
                
                this.increaseConcatMsg(currentMsgNum);
                if (this.countConcatMsgReceived() >= totalMsg) {
                    this.countMsgLimitExceed++;
                    this.resetConcatMsgVars();
                    this.msgLimitExceed = true;
                }
                
                env.copyValue(this.smsUserDataOffset, EXTENDED_BUFFER, msgBufferOffset, (short) (smsUserDataLength - 2));
                this.smsUserDataOffset += (short) (smsUserDataLength - 2);
            }
            
            if (this.msgLimitExceed) {
                this.shortBuffer[0] = 0;
                this.userDataMessageReference = -1;
                sfield_token255_descoff128_staticref32 = 0;
                sfield_token255_descoff135_staticref33 = false;
                this.processDataMessage((byte) 1, SMS_PP, flowState == SMS_SENDED);
            }
            
        }
    }
    
    /*
     * METHODS *** METHODS *** METHODS *** METHODS *** METHODS *** METHODS *** METHODS *** METHODS
     * METHODS *** METHODS *** METHODS *** METHODS *** METHODS *** METHODS *** METHODS *** METHODS
     * METHODS *** METHODS *** METHODS *** METHODS *** METHODS *** METHODS *** METHODS *** METHODS
     * METHODS *** METHODS *** METHODS *** METHODS *** METHODS *** METHODS *** METHODS *** METHODS
     * 
     */
    
    // method_token255_descoff1217
    /* вызывается с трех мест, processCellBroadcastPage, eventSmsPPDataDownload, eventMenuSelection */
    private void processDataMessage(byte cycleLimit, byte source, boolean var3) {
        // cycleLimit = 1;
        // processCellBroadcastPage: var2 =1 ; var3 = true; - CELL_BROADCAST
        // eventSmsPPDataDownload: var2 = 2; var3 = false; - SMS_PP
        // eventMenuSelection: var2 = 3; var3 = true; - MENU_SELECTION
        
        short var6 = (short) 1;
        boolean flag = false;
        
        for (byte i = 0; i < cycleLimit; i++) {
            byte result = this.function_DO_1(this.shortBuffer[0]);
            
            if (this.field_token28_descoff870) {
                var3 = true;
            }
            
            if (source != SMS_PP && source != MENU_SELECTION) {
                var6 = (short) 1; // ICB
            } else {
                var6 = (short) -1; // SMS or MENU
            }
            
            if (result == 0x10) { // 0x10 = 16
                if (this.field_token25_descoff849 != 2 && this.field_token25_descoff849 != 4) {
                    this.shortBuffer[5] = (short) (this.shortBuffer[0] + this.shortBuffer[3]);
                } else {
                    this.shortBuffer[5] = 552;
                    this.method_token255_descoff965(EXTENDED_BUFFER, (short) (this.shortBuffer[0] + this.shortBuffer[3]), EXTENDED_BUFFER, this.shortBuffer[5]);
                }
                
                if (source == SMS_PP && var3 && sfield_token255_descoff268_staticref56 != this.firstByteMarkerClass && !this.field_token28_descoff870) {
                    var3 = false;
                    sfield_token255_descoff135_staticref33 = true;
                }
                
                if (this.field_token27_descoff863 && var3 && source != MENU_SELECTION) {
                    this.playTone();
                }
                
                this.processShowMessageAndParseResult(this.field_token25_descoff849 != 0, (byte) var6, this.shortBuffer[5], var3, source == SMS_PP);
            } else {
                result = 0x12; // 0x12 = 18
                this.parsedMsgBufferOffset = -6;
            }
            
            if (this.parsedMsgBufferOffset >= 0) {
                result = this.doCommandFromBuffer(PARSED_MESSAGE, this.parsedMsgBufferOffset);
            }
            
            if (this.moreCycles) {
                cycleLimit++;
                this.shortBuffer[0] += this.shortBuffer[4];
            } else {
                this.shortBuffer[0] += this.shortBuffer[4];
                
                if ((this.shortBuffer[0] % 79) != 0) {
                    this.shortBuffer[0] += (short) (79 - (this.shortBuffer[0] % 79));
                }
            }
            
            if (this.parsedMsgBufferOffset == -7) {
                if (!this.moreCycles) {
                    this.resetVars();
                    return;
                }
                
                flag = true;
            }
            
            if (flag && !this.moreCycles) {
                this.resetVars();
                return;
            }
        }
        
        this.resetVars();
    }
    
    // method_token255_descoff1253
    /* вызывается только с одного места, с processDataMessage */
    private byte function_DO_1(short bufferOffset) {
        // cycle 1: bufferOffset = 0;
        byte[] tmpHeaderBuffer = { 0, 0, 0 };
        
        Util.arrayCopy(EXTENDED_BUFFER, bufferOffset, tmpHeaderBuffer, (short) 0, (short) 3);
        this.shortBuffer[3] = (short) (tmpHeaderBuffer[0] & 0xFF);
        this.shortBuffer[3]++;
        
        // true - if low byte is нечетный (1,3,5,7,9)
        this.moreCycles = (tmpHeaderBuffer[1] & 1) != 0;
        
        // -64 = 1100:0000
        // в tmpHeaderBuffer[1] старший бит должен быть 1,2 или 3 (0x32)
        // если биты 6 и 7 установлены, то выходим
        if ((tmpHeaderBuffer[1] & -64) != 0) {
            return -1;
        } else {
            byte bufferFirstValue = 0;
            byte bufferSecondValue = 0;
            short bufferThirdValue = 0;
            
            // 48 = 0x30
            // 32 = 0x20
            // 16 = 0x10
            switch ((tmpHeaderBuffer[1] & 0x30)) {
                case 0x10:
                case 0x30:
                    bufferFirstValue = EXTENDED_BUFFER[bufferOffset + this.shortBuffer[3]];
                    this.shortBuffer[4] = (short) (bufferOffset + this.shortBuffer[3] + 1);
                    
                    for (byte i = 0; i < bufferFirstValue; i++) {
                        
                        bufferSecondValue = EXTENDED_BUFFER[this.shortBuffer[4]];
                        bufferThirdValue = (short) (EXTENDED_BUFFER[this.shortBuffer[4] + bufferSecondValue] & 0xFF);
                        
                        this.shortBuffer[4]++;
                        
                        this.shortBuffer[4] = (short) (this.shortBuffer[4] + bufferSecondValue + bufferThirdValue);
                    }
                    
                    this.shortBuffer[4] -= bufferOffset;
                    if (this.function_DO_1_1(bufferOffset, (tmpHeaderBuffer[1] & 0x30) == 0x30, tmpHeaderBuffer[1], tmpHeaderBuffer[2])) {
                        return 0x10; // 0x10, 0001:0000
                    }
                    break;
                case 0x20:
                    if (!this.isAddressInSmsTpduExist) {
                        return -2; // 0xFE, 1111:1110
                    }
                    
                    this.shortBuffer[4] = this.shortBuffer[3];
                    byte var2 = this.processEachPartsOfMessage(bufferOffset, tmpHeaderBuffer[1], tmpHeaderBuffer[2]);
                    if (var2 == 0) {
                        return 32; // 0x20, 0010:0000
                    }
                    
                    if (var2 == 2) {
                        this.readAndDisplayText(PROMPT_INDEX_PHONE_NOT_SUPPORT);
                    }
            }
            
            return -1; // 0xFF, 1111:1111
        }
    }
    
    private byte method_token255_descoff1049(byte value) {
        short var3 = sfield_token255_descoff212_staticref45;
        if (value == 0) {
            return 1;
        } else {
            for (byte i = 0; i < var3; i++) {
                if (sfield_token255_descoff44_staticref8[i * 2] == value) {
                    if (sfield_token255_descoff44_staticref8[i * 2 + 1] == 1) {
                        return 1;
                    }
                    return 0;
                }
            }
            return 2;
        }
    }
    
    // method_token255_descoff12411
    /* вызывается только с одного места, с function_DO_1 */
    private boolean function_DO_1_1(short bufferOffset, boolean var2, byte headerSecondByte, byte headerThirdByte) {
        short var6 = 0;
        short var8 = 0;
        
        short var11 = 0;
        short var12 = 0;
        short var13 = 1;
        short var14 = 1;
        short var15 = 0;
        short var16 = 1;
        short var19 = 0;
        this.field_token31_descoff891 = false;
        // 0x06 - check bit 1 and 2
        this.field_token25_descoff849 = (byte) (headerSecondByte & 0x06); // 0x32 & 0x06 = 0x02
        byte thirdByteLowValue = (byte) (headerThirdByte & 0x0F); // 0x14 & 0x0F = 0x4
        
        short parsedOffset = 0;
        short offset = (short) (bufferOffset + 1 + 2); // offset = 3
        
        PARSED_MESSAGE[31] = 0;
        
        if (var2) {
            // EX[3] = 0x00
            if (this.method_token255_descoff1049(EXTENDED_BUFFER[offset++]) == 0) {
                return false;
            }
            // offset 4
        }
        
        if (this.field_token25_descoff849 == 2) {
            // 0x7F80 = 32640
            // EX[4] = 0x08 << 7 = 0x0400 & 0x7F80 = 0x0400
            this.field_token26_descoff856 = (short) ((EXTENDED_BUFFER[offset++] << 7) & 0x7F80);
            // offset 5
            
        } else if (this.field_token25_descoff849 == 4) {
            this.field_token26_descoff856 = Util.getShort(EXTENDED_BUFFER, offset);
            offset = (short) (offset + 2);
        }
        
        if ((headerThirdByte & 0x80) != 0) {// 0x80 = -128 (0x14 & 0x80 = 0)
            this.field_token27_descoff863 = true;
        } else {
            this.field_token27_descoff863 = false;
        }
        
        if ((headerThirdByte & 0x40) != 0) {// 0x40 = 64 (0x14 & 0x40 = 0)
            this.field_token28_descoff870 = true;
        } else {
            this.field_token28_descoff870 = false;
        }
        
        if ((headerThirdByte & 0x20) != 0) {// 0x20 = 32 (0x14 & 0x20 = 0)
            if ((EXTENDED_BUFFER[offset] & sfield_token255_descoff170_staticref39) == 0 && EXTENDED_BUFFER[offset] != 0) {
                return false;
            }
            offset++;
        }
        
        // !!!!!!!!!!!!!!!! return FALSE получается (сначало надо выставить sfield_token255_descoff205_staticref44 какое нить значение, возможно
        // предварительно по смс)
        if ((headerThirdByte & 0x10) != 0) {// 0x10 = 16 (0x14 & 0x10 = 0x10)
            // offset 5 = 0x48
            if ((EXTENDED_BUFFER[offset] & sfield_token255_descoff205_staticref44) == 0 && EXTENDED_BUFFER[offset] != 0) {
                return false;
            }
            offset++;
            // offset 6
        }
        
        byte shortNumberLen = 0;
        
        if (thirdByteLowValue > 0) { // 4
            // offset 6 = 0x00
            this.shortNumber1LenGlobal = EXTENDED_BUFFER[offset++];
            // offset 7
            if (this.shortNumber1LenGlobal > 0) {
                shortNumberLen = this.shortNumber1LenGlobal;
                if (this.shortNumber1LenGlobal > 11) {
                    this.shortNumber1LenGlobal = 11;
                }
                
                Util.arrayCopy(EXTENDED_BUFFER, offset, PARSED_MESSAGE, (short) 0, this.shortNumber1LenGlobal);
                offset = (short) (offset + shortNumberLen);
            }
            
            // offset 7 = 0x00
            this.shortNumber2LenGlobal = EXTENDED_BUFFER[offset++];
            // offset 8
            if (this.shortNumber2LenGlobal > 0) {
                shortNumberLen = this.shortNumber2LenGlobal;
                if (this.shortNumber2LenGlobal > 20) {
                    this.shortNumber2LenGlobal = 20;
                }
                
                Util.arrayCopy(EXTENDED_BUFFER, offset, PARSED_MESSAGE, (short) 11, this.shortNumber2LenGlobal);
                offset = (short) (offset + shortNumberLen);
            }
        }
        
        SIMView sv = SIMSystem.getTheSIMView();
        sv.select(SIMView.FID_MF);
        sv.select(SIMView.FID_DF_GSM);
        sv.select(DF_CELLTICK);
        sv.select(EF_APP_MESSAGES);
        
        if (sfield_token255_descoff331_staticref67 != 2 || flowState != READY) {
            
            short offsetBaseItem = this.findBaseItemsOffset(TAG_BASE_ITEM_NEXT);
            if (offsetBaseItem != -1) {
                // TAG_BASE_ITEM_NEXT = len 20, return len+1 = 21
                parsedOffset = (short) (parsedOffset + this.readSimMessageValueToBuffer(offsetBaseItem, PARSED_MESSAGE, (short) (32 + parsedOffset), true));
                short savedParsedOffset = parsedOffset; // = 21
                
                parsedOffset++;
                // parsedOffset = 22
                PARSED_MESSAGE[32 + parsedOffset] = (byte) 0x80; // 0x80 = -128;
                parsedOffset++;
                PARSED_MESSAGE[32 + savedParsedOffset] = (byte) (parsedOffset - savedParsedOffset - 1); // (22 - 21 - 1) = 0
                PARSED_MESSAGE[31]++;
            }
        }
        
        short limit = 5;
        short var9 = 0;
        boolean var17 = false;
        
        for (byte counter = 0; counter < thirdByteLowValue && counter < limit; counter++) {
            //offset 8 = 0xF0
            short nextByte = EXTENDED_BUFFER[offset];
            var9 = 0;
            var16 = 1;
            var14 = 1;
            var15 = 0;
            var6 = (short) (nextByte & 0x70); // 0x70 = 112 (0xF0 & 0x70 = 0x70)
            
            switch (var6) {
                case 0x00:
                case 0x20: // 0x20 = 32
                case 0x30: // 0x30 = 48
                case 0x50: // 0x50 = 80
                case 0x60: // 0x60 = 96
                    if (var6 == 0 && sfield_token255_descoff163_staticref38 != 2 && sfield_token255_descoff163_staticref38 != 3) {
                        var14 = 0;
                    }
                    
                    var9 = (short) (1 + (nextByte & 0x0F)); // 0x0F = 15
                    var9 = (short) (var9 + 1 + (EXTENDED_BUFFER[offset + var9] & 0x7F)); // 0x7F = 127
                    break;
                case 0x10: // 0x10 = 16
                case 0x40: // 0x40 = 64
                    if (var6 == 0x40) {
                        this.field_token31_descoff891 = true;
                    }
                    
                    var9 = (short) (1 + (nextByte & 0x0F)); // 0x0F = 15
                    break;
                case 0x70: // 0x70 = 112
                    // offset 11+2 = 0x11
                    var6 = EXTENDED_BUFFER[offset + 2];
                    switch (var6) {
                        case 2:
                        case 3:
                            var9 = 1;
                            
                            for (var19 = 0; var9 < EXTENDED_BUFFER[offset + 1]; var19++) {
                                var9 = (short) (var9 + EXTENDED_BUFFER[offset + var9 + 1 + 1] + 1);
                            }
                            
                            if (sfield_token255_descoff191_staticref42 && var19 >= 2) {
                                var15 = 1;
                            }
                            
                            var19 = 0;
                            var9 = EXTENDED_BUFFER[offset + 1];
                            
                            if (EXTENDED_BUFFER[offset + var9 + 4] == 4) {
                                var19 = 1;
                            }
                            
                            var9 = 1;
                    }
                    
                    if (var15 != 0) {
                        break;
                    }
                default:
                    limit++;
                    var16 = 0;
                    offset = (short) (offset + 2 + EXTENDED_BUFFER[offset + 1]);
                    // offset 11+2+06 = 0xA0
            }
            
            if (var16 != 0) {
                if (var14 != 0) {
                    PARSED_MESSAGE[31]++;
                }
                
                var17 = ((nextByte & 0x80) == 0 ? false : true); // -128 = 0x80
                var13 = 1;
                short var24;
                if (!var17 && (var15 == 0 || var6 != 2 && var6 != 3 || var19 != 1)) {
                    if (var14 != 0) {
                        if (var15 != 0 && (var6 == 2 || var6 == 3)) {
                            var12 = (short) (EXTENDED_BUFFER[offset + var9 + 2] + 1);
                            var11 = this.findBaseItemsOffset(EXTENDED_BUFFER[offset + var9 + 2 + var12 + 1]);
                        } else {
                            var11 = this.findBaseItemsOffset(EXTENDED_BUFFER[offset + var9]);
                        }
                        
                        if (var11 != -1) {
                            parsedOffset = (short) (parsedOffset + this.readSimMessageValueToBuffer(var11, PARSED_MESSAGE, (short) (32 + parsedOffset), true));
                        } else {
                            var13 = 0;
                            PARSED_MESSAGE[31]--;
                        }
                    }
                } else {
                    var24 = 0x80; // -128 = 0x80
                    short var25 = (short) (this.field_token25_descoff849 == 0 ? 0 : 1);
                    if (var15 != 0 && (var6 == 2 || var6 == 3)) {
                        var9 = (short) (var9 + 2);
                        var12 = (short) (EXTENDED_BUFFER[offset + var9] + 1);
                        var9 = (short) (var9 + var12);
                        if (var19 == 1) {
                            var12 = (short) (EXTENDED_BUFFER[offset + var9] + 1);
                            var9 = (short) (var9 + var12 + 1 + 1 + 1);
                            var12 = (short) (EXTENDED_BUFFER[offset + var9] + 1);
                            var9 = (short) (var9 + var12);
                            var8 = EXTENDED_BUFFER[offset + var9];
                        } else {
                            var8 = EXTENDED_BUFFER[offset + var9];
                        }
                    } else {
                        var8 = (short) EXTENDED_BUFFER[offset + var9];
                        var12 = (short) (var8 + 1);
                    }
                    
                    if (var8 > 40) {
                        var8 = 40;
                    }
                    
                    if (this.field_token25_descoff849 != 2 && this.field_token25_descoff849 != 4) {
                        if (var14 != 0) {
                            if (var25 != 0) {
                                PARSED_MESSAGE[32 + parsedOffset] = (byte) (var8 + 1);
                                PARSED_MESSAGE[32 + parsedOffset + 1] = (byte) var24;
                                parsedOffset = (short) (parsedOffset + 2);
                            } else {
                                PARSED_MESSAGE[32 + parsedOffset] = (byte) ((short) var8);
                                parsedOffset++;
                            }
                            
                            Util.arrayCopy(EXTENDED_BUFFER, (short) (offset + var9 + 1), PARSED_MESSAGE, (short) (32 + parsedOffset), var8);
                            if (var15 == 0) {
                                var9 = (short) (var9 + var12);
                            }
                            
                            parsedOffset = (short) (parsedOffset + var8);
                        }
                    } else {
                        if (var14 != 0) {
                            var8 = this.method_token255_descoff977(EXTENDED_BUFFER, (short) (offset + var9), PARSED_MESSAGE, (short) (32 + parsedOffset + 2));
                            PARSED_MESSAGE[32 + parsedOffset] = (byte) ((var8 > 40 ? 40 : var8) + 1);
                            PARSED_MESSAGE[32 + parsedOffset + 1] = (byte) var24;
                            parsedOffset = (short) (parsedOffset + PARSED_MESSAGE[32 + parsedOffset] + 1);
                        }
                        
                        if (var15 == 0) {
                            var9 = (short) (var9 + var12);
                        }
                    }
                }
                
                if (var14 == 0) {
                    var13 = 0;
                }
                
                if (var13 != 0) {
                    var11 = parsedOffset;
                    parsedOffset++;
                    PARSED_MESSAGE[32 + parsedOffset] = (byte) var6;
                    parsedOffset++;
                }
                
                var8 = (short) (nextByte & 15);
                shortNumberLen = (byte) var8;
                if (var8 > 11) {
                    var8 = 11;
                }
                
                if (var6 == 16 || var6 == 0 || var6 == 48 || var6 == 32 || var6 == 96 || var6 == 80) {
                    if (var13 != 0) {
                        if (var8 > 0) {
                            PARSED_MESSAGE[32 + parsedOffset] = (byte) (var8 + 1);
                            if (var6 != 16) {
                                var24 = (short) ((var8 - 1) * 2);
                                if ((EXTENDED_BUFFER[offset + 1 + var8 - 1] & -16) == -16) {
                                    var24--;
                                }
                            } else {
                                var24 = var8;
                            }
                            
                            parsedOffset++;
                            PARSED_MESSAGE[32 + parsedOffset] = (byte) var24;
                            parsedOffset++;
                            Util.arrayCopy(EXTENDED_BUFFER, (short) (offset + 1), PARSED_MESSAGE, (short) (32 + parsedOffset), var8);
                            parsedOffset = (short) (parsedOffset + var8);
                        } else {
                            PARSED_MESSAGE[32 + parsedOffset] = (byte) (this.shortNumber1LenGlobal + 1);
                            if (var6 != 16) {
                                var24 = (short) ((this.shortNumber1LenGlobal - 1) * 2);
                                if ((PARSED_MESSAGE[this.shortNumber1LenGlobal - 1] & 0xF0) == 0xF0) { // 0xF0 = -16
                                    var24--;
                                }
                            } else {
                                var24 = this.shortNumber1LenGlobal;
                            }
                            
                            parsedOffset++;
                            PARSED_MESSAGE[32 + parsedOffset] = (byte) var24;
                            parsedOffset++;
                            Util.arrayCopy(PARSED_MESSAGE, (short) 0, PARSED_MESSAGE, (short) (32 + parsedOffset), this.shortNumber1LenGlobal);
                            parsedOffset = (short) (parsedOffset + this.shortNumber1LenGlobal);
                        }
                    }
                    
                    offset = (short) (offset + 1 + shortNumberLen);
                }
                
                if (var6 == 0 || var6 == 48 || var6 == 32 || var6 == 96 || var6 == 80) {
                    var24 = EXTENDED_BUFFER[offset];
                    var8 = (short) (var24 & 0x7F); // 0x7F = 127
                    shortNumberLen = (byte) var8;
                    if (var8 > 20) {
                        var8 = 20;
                    }
                    
                    if (var13 != 0) {
                        if (var8 > 0) {
                            Util.arrayCopy(EXTENDED_BUFFER, (short) (offset + 1), PARSED_MESSAGE, (short) (32 + parsedOffset + 1), var8);
                        } else {
                            var8 = this.shortNumber2LenGlobal;
                            Util.arrayCopy(PARSED_MESSAGE, (short) 11, PARSED_MESSAGE, (short) (32 + parsedOffset + 1), var8);
                        }
                        
                        if ((var24 & 0x80) != 0) { // -128 = 0x80
                            PARSED_MESSAGE[32 + parsedOffset + var8 + 1] = 32;
                            PARSED_MESSAGE[32 + parsedOffset + var8 + 2] = (byte) (counter + 1 + 48);
                            var8 = (short) (var8 + 2);
                        }
                        
                        if (var6 == 0) {
                            PARSED_MESSAGE[32 + parsedOffset + var8 + 1] = 32;
                            PARSED_MESSAGE[32 + parsedOffset + var8 + 2] = (byte) (sfield_token255_descoff163_staticref38 + 48);
                            var8 = (short) (var8 + 2);
                        }
                        
                        PARSED_MESSAGE[32 + parsedOffset] = (byte) var8;
                        parsedOffset = (short) (parsedOffset + var8 + 1);
                    }
                    
                    offset = (short) (offset + shortNumberLen + 1);
                }
                
                if (var6 == 2 || var6 == 3) {
                    var8 = EXTENDED_BUFFER[offset + 1 + 2];
                    shortNumberLen = (byte) var8;
                    if (var8 > 20) {
                        var8 = 20;
                    }
                    
                    if (var13 != 0) {
                        if (var8 > 0) {
                            Util.arrayCopy(EXTENDED_BUFFER, (short) (offset + 1 + 3), PARSED_MESSAGE, (short) (32 + parsedOffset + 1), var8);
                        } else {
                            var8 = this.shortNumber2LenGlobal;
                            Util.arrayCopy(PARSED_MESSAGE, (short) 11, PARSED_MESSAGE, (short) (32 + parsedOffset + 1), var8);
                        }
                        
                        PARSED_MESSAGE[32 + parsedOffset] = (byte) var8;
                        parsedOffset = (short) (parsedOffset + var8 + 1);
                    }
                }
                
                if (var15 != 0) {
                    offset = (short) (offset + EXTENDED_BUFFER[offset + 1] + 1 + 1);
                    if (var19 == 1) {
                        offset = (short) (offset + EXTENDED_BUFFER[offset + 1] + 1 + 1);
                        limit++;
                        counter++;
                    }
                } else {
                    if (var6 == 64) {
                        offset++;
                    }
                    
                    if (var17) {
                        offset = (short) (offset + var12);
                    } else {
                        offset++;
                    }
                }
                
                if (var13 != 0) {
                    PARSED_MESSAGE[32 + var11] = (byte) (parsedOffset - var11 - 1);
                }
            }
        }
        
        if (flowState != READY || this.field_token28_descoff870) {
            short itemOffset = this.findBaseItemsOffset(TAG_BASE_ITEM_MAIN);
            if (itemOffset != -1) {
                parsedOffset = (short) (parsedOffset + this.readSimMessageValueToBuffer(itemOffset, PARSED_MESSAGE, (short) (32 + parsedOffset), true));
                short savedParsedOffset = parsedOffset;
                
                parsedOffset++;
                PARSED_MESSAGE[32 + parsedOffset] = -112; // 0x90
                parsedOffset++;
                PARSED_MESSAGE[32 + savedParsedOffset] = (byte) (parsedOffset - savedParsedOffset - 1);
                PARSED_MESSAGE[31]++;
            }
        }
        
        return true;
    }
    
    private void resetConcatMsgVars() {
        for (short i = 0; i < this.concatMsgMapper.length; i++) {
            if (this.concatMsgMapper[i]) {
                this.concatMsgCounter--;
            }
            this.concatMsgMapper[i] = false;
        }
    }
    
    private boolean isConcatMsgAlreadyMapped(byte currentMsgNum) {
        byte msgIndex = (byte) (currentMsgNum - 1);
        if (msgIndex >= 0 && msgIndex <= this.concatMsgMapper.length)
            return this.concatMsgMapper[msgIndex];
        else
            return false;
    }
    
    private void increaseConcatMsg(byte currentMsgNum) {
        byte i = (byte) (currentMsgNum - 1);
        if (i >= 0 && i < this.concatMsgMapper.length) {
            if (!this.concatMsgMapper[i]) {
                this.concatMsgCounter++;
            }
            
            this.concatMsgMapper[i] = true;
        }
    }
    
    private byte countConcatMsgReceived() {
        byte ret = 0;
        for (byte i = 0; i < this.concatMsgMapper.length; i++) {
            if (this.concatMsgMapper[i]) {
                ret++;
            }
        }
        return ret;
    }
    
    private void resetVars() {
        this.shortBuffer[2] = 0;
        
        this.concatMsgCounter = 0;
        this.resetConcatMsgVars();
        this.countMsgLimitExceed = 0;
        this.msgLimitExceed = false;
        this.userDataMessageReference = -1;
    }
}
