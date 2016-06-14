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
    // private static byte sfield_token255_descoff338_staticref68 = 1;
    
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
    
    /* UNKNOWN VARS */
    
    public boolean[]            field_token15_descoff779;
    
    private static short        sfield_token255_descoff653_staticref117 = 2;
    
    private boolean             field_token27_descoff863;
    
    private static boolean      sfield_token255_descoff219_staticref46;
    private static byte         sfield_token255_descoff268_staticref56;
    private static short        sfield_token255_descoff142_staticref34;
    
    private static boolean      sfield_token255_descoff191_staticref42;
    private static byte         sfield_token255_descoff303_staticref63  = 4;
    
    private static boolean      sfield_token255_descoff639_staticref114 = true;
    public final byte           field_token2_descoff688                 = 6;
    private byte                field_token25_descoff849                = 0;
    
    // private byte field_token32_descoff898 = 0;
    public final byte           field_token7_descoff723                 = 1;
    public final byte           field_token14_descoff772                = 5;
    private static byte         sfield_token255_descoff128_staticref32;
    private static boolean      sfield_token255_descoff177_staticref40;
    private static byte         sfield_token255_descoff212_staticref45;
    private static short        sfield_token255_descoff261_staticref54;
    public final byte           field_token5_descoff709                 = 2;
    public final byte           field_token12_descoff758                = 3;
    private static boolean      sfield_token255_descoff632_staticref113 = true;
    public final byte           field_token1_descoff681                 = 3;
    
    private boolean             field_token24_descoff842;
    
    private boolean             field_token31_descoff891                = false;
    private static short        sfield_token255_descoff247_staticref50;
    // private static boolean sfield_token255_descoff296_staticref62 = true;
    
    private static byte         sfield_token255_descoff170_staticref39;
    private static byte         sfield_token255_descoff331_staticref67  = 1;
    private static short        sfield_token255_descoff667_staticref121 = 1;
    
    private byte                field_token29_descoff877                = 0;
    public final byte           field_token4_descoff702                 = 1;
    public final byte           field_token11_descoff751                = 2;
    
    // private static boolean sfield_token255_descoff240_staticref49;
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
    // private short field_token23_descoff835 = -6;
    
    private byte                field_token30_descoff884                = 0;
    // private static boolean sfield_token255_descoff289_staticref61 = true;
    
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
        
        this.method_token255_descoff1397();
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
    
    private void method_token255_descoff1529(boolean var1, byte[] var2, short var3, short var4) {
        short var5 = (short) 0;
        short var6 = (short) 601;
        short var7 = (short) -128;
        short var8;
        if (var1) {
            short var10001 = (short) var6;
            var6 = (short) ((short) ((short) var6 + 1));
            sfield_token255_descoff58_staticref12[var10001] = (byte) ((short) var7);
            
            for (var8 = (short) 0; (short) var8 < 20; var8 = (short) ((byte) ((short) ((short) var8 + 2)))) {
                sfield_token255_descoff58_staticref12[(short) ((short) var6 + (short) var8)] = 0;
                sfield_token255_descoff58_staticref12[(short) ((short) ((short) var6 + (short) var8) + 1)] = 32;
            }
            
            var5 = (short) ((byte) ((short) var4 < 20 ? (short) var4 : 20));
        } else {
            for (var8 = (short) 0; (short) var8 < 21; var8 = (short) ((byte) ((short) ((short) var8 + 1)))) {
                sfield_token255_descoff58_staticref12[(short) ((short) var6 + (short) var8)] = 32;
            }
            
            var5 = (short) ((byte) ((short) var4 < 10 ? (short) var4 : 10));
        }
        
        sfield_token255_descoff58_staticref12[600] = (byte) ((short) ((short) var5 + (var1 ? 1 : 0)));
        Util.arrayCopy(var2, (short) var3, sfield_token255_descoff58_staticref12, (short) var6, (short) var5);
    }
    
    private void method_token255_descoff1397() {
        for (byte i = 0; i < sfield_token255_descoff100_staticref24.length; i++) {
            sfield_token255_descoff100_staticref24[i] = 1;
        }
        
    }
    
    // method_token255_descoff1013
    private byte displayText(byte dcs, byte[] text, short offset, short length) {
        ProactiveHandler proHandlr = ProactiveHandler.getTheHandler();
        proHandlr.initDisplayText((byte) 0x80, dcs, text, offset, length); // byte qualifier, byte dcs, byte[] buffer, short offset, short length
        return proHandlr.send();
    }
    
    // private byte method_token255_descoff1013(byte var1, byte dcs, byte[] var3, short var4, short var5) {
    // ProactiveHandler var6 = ProactiveHandler.getTheHandler();
    // var6.initDisplayText((byte) ((short) var1), dcs, var3, (short) var4, (short) var5);
    // return var6.send();
    // }
    
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
        sfield_token255_descoff79_staticref18[offset++] = 0x11; // 0x11 0001:0001 (TP-MTI bit 0 and bit 1 - submit type) (TP-VPF bit 3 and bit 4 -
                                                                // relative format)
        sfield_token255_descoff79_staticref18[offset++] = (byte) (getSmsMessageReferece() + 1); // TP-MR msg reference
        
        short len = (short) (buffer[index++] & 0xFF);
        
        // 1. first 1 byte that inform the length of address
        // 2. 2nd 1 byte that inform TON/NPI
        // 3. the rest is address in BCD swapped format
        offset = Util.arrayCopy(buffer, index, sfield_token255_descoff79_staticref18, offset, len); // TP-DA
        index = (short) (index + len);
        
        sfield_token255_descoff79_staticref18[offset++] = 0x00; // TP-PID
        sfield_token255_descoff79_staticref18[offset++] = dcsValue; // TP-DCS
        sfield_token255_descoff79_staticref18[offset++] = validatePeriod; // TP-VP
        
        len = buffer[index++];
        short savedOffset = offset;
        
        if (dcsValue == DCS_8_BIT_DATA && sfield_token255_descoff219_staticref46) {
            sfield_token255_descoff79_staticref18[offset++] = 0x00;
            sfield_token255_descoff79_staticref18[offset++] = 0x4D;
            sfield_token255_descoff79_staticref18[offset++] = 0x43;
            sfield_token255_descoff79_staticref18[offset++] = 0x20;
            sfield_token255_descoff79_staticref18[offset++] = 0x02;
            sfield_token255_descoff79_staticref18[offset++] = (byte) len; // TP-UDL
        } else {
            sfield_token255_descoff79_staticref18[offset++] = (byte) len; // TP-UDL
        }
        
        offset = Util.arrayCopy(buffer, index, sfield_token255_descoff79_staticref18, offset, len); // TP-UD
        if (dcsValue == DCS_8_BIT_DATA && sfield_token255_descoff219_staticref46) {
            sfield_token255_descoff79_staticref18[offset++] = (byte) (4 + sfield_token255_descoff212_staticref45 * 2);
            sfield_token255_descoff79_staticref18[offset++] = 0x50;
            sfield_token255_descoff79_staticref18[offset++] = 0x43;
            sfield_token255_descoff79_staticref18[offset++] = 0x20;
            sfield_token255_descoff79_staticref18[offset++] = sfield_token255_descoff212_staticref45;
            offset = Util.arrayCopy(sfield_token255_descoff44_staticref8, (short) 0, sfield_token255_descoff79_staticref18, offset, (short) (sfield_token255_descoff212_staticref45 * 2));
        }
        
        sfield_token255_descoff79_staticref18[savedOffset] = (byte) (offset - savedOffset - 1); // TP-UDL
        
        sfield_token255_descoff268_staticref56 = this.method_token255_descoff1037(sfield_token255_descoff79_staticref18, (short) (savedOffset + 1), sfield_token255_descoff79_staticref18[savedOffset]);
        
        ProactiveHandler proHandl = ProactiveHandler.getTheHandler();
        // qualifier
        // bit 1:
        // 0 = packing not required;
        // 1 = SMS packing by the ME required.
        // bits 2 to 8: = 0 RFU.
        byte qualifier = (byte) (dcsValue == DCS_DEFAULT_ALPHABET ? 1 : 0);
        
        proHandl.init(PRO_CMD_SEND_SHORT_MESSAGE, qualifier, DEV_ID_NETWORK);
        proHandl.appendTLV(TAG_SMS_TPDU, sfield_token255_descoff79_staticref18, (short) 0, offset);
        byte result = proHandl.send();
        if (result == RES_CMD_PERF) {
            sfield_token255_descoff219_staticref46 = false;
            ;
            updateSmsMessageReferece(sfield_token255_descoff79_staticref18[1]);
            return true;
        } else {
            return false;
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
    private void processShowMessageAndParseResult(boolean isCyrilic, byte var2, short var3, boolean var4, boolean var5) {
        short var6 = (short) 0;
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
            for (var11 = (short) 0; (short) var11 != (short) var2; var11 = (short) ((byte) ((short) ((short) var11 + 1)))) {
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
                        var6 = (short) Util.makeShort((byte) 0, this.byteBuffer[(short) ((short) var12 - 1)]);
                    } else {
                        var6 = (short) Util.getShort(sfield_token255_descoff58_staticref12, (short) (622 + (short) ((short) var10 * 2)));
                        var10 = (short) ((byte) ((short) ((short) var10 + 1)));
                    }
                    
                    bufferMessageOffset = (short) (bufferMessageOffset + var12);
                    if (var5 || this.field_token31_descoff891) {
                        this.method_token255_descoff1529(isCyrilic, EXTENDED_BUFFER, bufferMessageOffset, (short) var6);
                        this.field_token31_descoff891 = false;
                        if (var5) {
                            this.method_token255_descoff1517(sfield_token255_descoff114_staticref28, this.shortBuffer[0], this.shortBuffer[4], false);
                        }
                    }
                    
                    if (!var4) {
                        return;
                    }
                    
                    var5 = false;
                    if (isCyrilic) {
                        displayTextRes = this.displayText(dataCoding, EXTENDED_BUFFER, bufferMessageOffset, (short) var6 > 159 ? 158 : (short) var6);
                    } else {
                        displayTextRes = this.displayText(dataCoding, EXTENDED_BUFFER, bufferMessageOffset, (short) var6 > 159 ? 159 : (short) var6);
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
                    
                    bufferMessageOffset = (short) (bufferMessageOffset + var6);
                }
            }
            
        }
    }
    
    // private void processResultOfDisplayText(byte displayTextRes) {
    // switch (displayTextRes) {
    // case RES_CMD_PERF:
    // this.resetVars();
    //
    // this.parsedMsgBufferOffset = this.findParsedMessageBufferOffset();
    //
    // if (this.parsedMsgBufferOffset != -9 && this.parsedMsgBufferOffset != -3 && this.parsedMsgBufferOffset != -8 && this.parsedMsgBufferOffset !=
    // -5) {
    //
    // if (this.parsedMsgBufferOffset != -6 && this.parsedMsgBufferOffset < 0) {
    // this.parsedMsgBufferOffset = -4;
    // }
    //
    // } else {
    // flowState = READY;
    // this.parsedMsgBufferOffset = -7;
    // }
    // break;
    // case RES_CMD_PERF_SESSION_TERM_USER:
    // this.parsedMsgBufferOffset = -7;
    // break;
    // case RES_CMD_PERF_BACKWARD_MOVE_REQ:
    // flowState = READY;
    // this.parsedMsgBufferOffset = -7;
    // break;
    // default:
    // this.parsedMsgBufferOffset = -7;
    // }
    // }
    
    // method_token255_descoff11811
    /* вызывается только с одного места, с processShowMessageAndParseResult */
    private void processResultOfDisplayText(byte displayTextRes) {
        // if ((short) var1 == 16) {
        // this.field_token21_descoff821 = true;
        // }
        
        switch (displayTextRes) {
            case RES_CMD_PERF:
                this.resetVars();
                this.parsedMsgBufferOffset = this.method_token255_descoff917(sfield_token255_descoff58_staticref12, (short) 31);
                
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
    
    private boolean method_token255_descoff1205(byte var1, boolean var2) {
        for (short var3 = (short) 0; (short) var3 < sfield_token255_descoff100_staticref24.length; var3 = (short) ((byte) ((short) ((short) var3 + 1)))) {
            if (sfield_token255_descoff100_staticref24[(short) var3] == (short) var1) {
                return false;
            }
        }
        
        if (var2) {
            sfield_token255_descoff100_staticref24[sfield_token255_descoff261_staticref54] = (byte) ((short) var1);
            if (++sfield_token255_descoff261_staticref54 >= sfield_token255_descoff100_staticref24.length) {
                sfield_token255_descoff261_staticref54 = 0;
            }
        }
        
        return true;
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
    
    private boolean method_token255_descoff1073(byte[] var1, short var2) {
        // short var3 = (short) 0;
        short var4 = (short) 0;
        ProactiveHandler var5 = ProactiveHandler.getTheHandler();
        this.readSimMessageValueToBuffer(MESSAGES_OFFSET_HOLDER[PROMPT_INDEX_INPUT_NUMBER], sfield_token255_descoff79_staticref18, (short) 0, false);
        var5.initGetInput((byte) 0, dcs, sfield_token255_descoff79_staticref18, (short) 1, sfield_token255_descoff79_staticref18[0], (short) 1, (short) 19);
        short var6 = (short) var5.send();
        if ((short) var6 != 0) {
            return false;
        } else {
            ProactiveResponseHandler var7 = ProactiveResponseHandler.getTheHandler();
            var4 = (short) ((byte) var7.getTextStringLength());
            short var10001 = (short) var2;
            var2 = (short) ((short) ((short) var2 + 1));
            var1[var10001] = (byte) ((short) ((short) var4 + 1));
            var10001 = (short) var2;
            var2 = (short) ((short) ((short) var2 + 1));
            var1[var10001] = 32;
            var7.copyTextString(var1, (short) var2);
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
        
        sv.readBinary(MESSAGES_OFFSET_HOLDER[index], sfield_token255_descoff79_staticref18, (short) 0, (short) (len + 1));
        ProactiveHandler pro = ProactiveHandler.getTheHandler();
        pro.initDisplayText((byte) 0x80, dcs, sfield_token255_descoff79_staticref18, (short) 1, len);
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
            Util.setShort(sfield_token255_descoff58_staticref12, (short) (622 + (short) ((short) var12 * 2)), (short) var7);
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
    
    private byte method_token255_descoff1049(byte var1) {
        short var2 = (short) 0;
        short var3 = (short) sfield_token255_descoff212_staticref45;
        if ((short) var1 == 0) {
            return (byte) 1;
        } else {
            for (var2 = (short) 0; (short) var2 < (short) var3; var2 = (short) ((byte) ((short) ((short) var2 + 1)))) {
                if (sfield_token255_descoff44_staticref8[(short) ((short) var2 * 2)] == (short) var1) {
                    if (sfield_token255_descoff44_staticref8[(byte) ((short) ((short) ((short) var2 * 2) + 1))] == 1) {
                        return (byte) 1;
                    }
                    
                    return (byte) 0;
                }
            }
            
            return (byte) 2;
        }
    }
    
    private boolean method_token255_descoff1517(byte[] var1, short var2, short var3, boolean var4) {
        short var5 = (short) (var1 == sfield_token255_descoff114_staticref28 ? sfield_token255_descoff660_staticref119 : sfield_token255_descoff667_staticref121);
        short var6 = (short) sfield_token255_descoff58_staticref12[600];
        short var7 = (short) ((short) ((short) ((short) ((short) ((short) var3 + (short) var6) + 1) + 2) - (short) (577 - (short) var5)));
        if ((short) var7 > 0) {
            if (var4 && this.readAndDisplayText(PROMPT_INDEX_REMOVE_MESSAGES) != 0) {
                return false;
            }
            
            if (!this.method_token255_descoff989(var1, (short) var7, false)) {
                return false;
            }
            
            var5 = (short) (var1 == sfield_token255_descoff114_staticref28 ? sfield_token255_descoff660_staticref119 : sfield_token255_descoff667_staticref121);
        }
        
        while ((short) (9 + (short) (24 * (short) (var1[0] + 1))) > 255) {
            if (!this.method_token255_descoff989(var1, (short) 2, false)) {
                return false;
            }
            
            var5 = (short) (var1 == sfield_token255_descoff114_staticref28 ? sfield_token255_descoff660_staticref119 : sfield_token255_descoff667_staticref121);
        }
        
        Util.arrayCopy(sfield_token255_descoff58_staticref12, (short) 600, var1, (short) var5, (short) ((short) var6 + 1));
        Util.setShort(var1, (short) ((short) ((short) var5 + (short) var6) + 1), (short) var3);
        Util.arrayCopyNonAtomic(EXTENDED_BUFFER, (short) var2, var1, (short) ((short) ((short) ((short) var5 + (short) var6) + 1) + 2), (short) var3);
        var5 = (short) ((short) ((short) var5 + (short) ((short) ((short) ((short) var6 + (short) var3) + 1) + 2)));
        if (var1 == sfield_token255_descoff114_staticref28) {
            sfield_token255_descoff660_staticref119 = (short) var5;
        } else {
            sfield_token255_descoff667_staticref121 = (short) var5;
        }
        
        var1[0] = (byte) ((short) (var1[0] + 1));
        return true;
    }
    
    private byte method_token255_descoff1229(short var1, byte var2, byte var3) {
        short var4 = (short) 0;
        short var5 = (short) 0;
        short var6 = (short) 0;
        short var7 = (short) 0;
        short var8 = (short) 0;
        short var9 = (short) ((short) ((short) var3 & 15));
        short var11 = (short) (sfield_token255_descoff149_staticref36 ? 1 : 0);
        short var10 = (short) ((short) ((short) ((short) var1 + 1) + 2));
        
        for (short var12 = (short) 0; (short) var12 < (short) var9; var12 = (short) ((byte) ((short) ((short) var12 + 1)))) {
            short var13 = (short) EXTENDED_BUFFER[(short) var10];
            short var14 = (short) ((short) (EXTENDED_BUFFER[(short) ((short) var10 + 1)] & 255));
            var10 = (short) (var10 + 2);
            short var15;
            SIMView var16;
            short var10001;
            short var20;
            switch ((short) var13) {
                case 3:
                    sfield_token255_descoff632_staticref113 = true;
                    Util.arrayCopy(imeiBuffer, (short) 0, imeiBufferToCompare, (short) 0, (short) 8);
                    var15 = (short) (sfield_token255_descoff177_staticref40 ? 1 : 0);
                    sfield_token255_descoff149_staticref36 = false;
                    sfield_token255_descoff163_staticref38 = EXTENDED_BUFFER[(short) ((short) var10 + 5)];
                    sfield_token255_descoff170_staticref39 = EXTENDED_BUFFER[(short) ((short) var10 + 6)];
                    sfield_token255_descoff639_staticref114 = (short) (EXTENDED_BUFFER[(short) ((short) var10 + 7)] & -128) != 0;
                    sfield_token255_descoff177_staticref40 = (short) (EXTENDED_BUFFER[(short) ((short) var10 + 7)] & 64) != 0;
                    sfield_token255_descoff184_staticref41 = (short) (EXTENDED_BUFFER[(short) ((short) var10 + 7)] & 32) != 0;
                    sfield_token255_descoff191_staticref42 = (short) (EXTENDED_BUFFER[(short) ((short) var10 + 7)] & 16) != 0;
                    if (!sfield_token255_descoff177_staticref40) {
                        sfield_token255_descoff317_staticref65 = sfield_token255_descoff310_staticref64;
                        sfield_token255_descoff310_staticref64 = 2;
                        sfield_token255_descoff247_staticref50 = sfield_token255_descoff254_staticref52 = sfield_token255_descoff646_staticref115;
                        sfield_token255_descoff135_staticref33 = false;
                        this.workWithICBchannel(true);
                        return (byte) 2;
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
                    
                    var10 = (short) ((short) ((short) var10 + (short) var14));
                    break;
                case 4:
                    var15 = (short) 0;
                    var20 = (short) 0;
                    var4 = (short) (EXTENDED_BUFFER[(short) var10] > 10 ? 10 : EXTENDED_BUFFER[(short) var10]);
                    var7 = (short) ((short) ((short) var10 + 1));
                    
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
                    var10 = (short) ((short) ((short) var10 + (short) var14));
                    break;
                case 5:
                    var15 = (short) ((short) var10);
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
                    
                    var10 = (short) ((short) ((short) var10 + (short) var14));
                    break;
                case 6:
                    sfield_token255_descoff303_staticref63 = EXTENDED_BUFFER[(short) ((short) var10 + 2)];
                    if ((short) var14 > 4) {
                        sfield_token255_descoff646_staticref115 = Util.makeShort((byte) 0, EXTENDED_BUFFER[(short) ((short) var10 + 3)]);
                        sfield_token255_descoff653_staticref117 = Util.makeShort((byte) 0, EXTENDED_BUFFER[(short) ((short) var10 + 4)]);
                    }
                    
                    var10 = (short) ((short) ((short) var10 + (short) var14));
                    break;
                case 7:
                    // update ON SIM short number on 15 index
                    
                    var15 = (short) EXTENDED_BUFFER[(short) var10];
                    if ((short) var15 > 1 && (short) var15 <= 12) {
                        Util.arrayCopy(EXTENDED_BUFFER, (short) var10, BUFFER_FOR_NUMBERS, (short) 0, (short) ((short) var15 + 1));
                        var16 = SIMSystem.getTheSIMView();
                        var16.select(SIMView.FID_MF);
                        var16.select(SIMView.FID_DF_GSM);
                        var16.select(DF_CELLTICK);
                        var16.select(EF_APP_MESSAGES);
                        var16.updateBinary((short) 15, EXTENDED_BUFFER, (short) var10, (short) ((short) var15 + 1));
                    }
                    
                    var10 = (short) ((short) ((short) var10 + (short) var14));
                    break;
                case 8:
                    // update ON SIM icb channel on 29 index
                    if (sfield_token255_descoff310_staticref64 == 1) {
                        this.workWithICBchannel(false);
                    }
                    
                    CHANNEL_2 = Util.getShort(EXTENDED_BUFFER, (short) var10);
                    CHANNEL_1 = Util.makeShort(EXTENDED_BUFFER[(short) ((short) var10 + 1)], EXTENDED_BUFFER[(short) var10]);
                    if (sfield_token255_descoff310_staticref64 == 1) {
                        this.workWithICBchannel(true);
                    }
                    
                    SIMView var19 = SIMSystem.getTheSIMView();
                    var19.select(SIMView.FID_MF);
                    var19.select(SIMView.FID_DF_GSM);
                    var19.select(DF_CELLTICK);
                    var19.select(EF_APP_MESSAGES);
                    var19.updateBinary((short) 29, EXTENDED_BUFFER, (short) var10, (short) 2);
                    var10 = (short) ((short) ((short) var10 + (short) var14));
                    break;
                case 9:
                    if (EXTENDED_BUFFER[(short) var10] == 1 && sfield_token255_descoff184_staticref41) {
                        isAllowPlaySound = true;
                    } else if (EXTENDED_BUFFER[(short) var10] == 2) {
                        isAllowPlaySound = false;
                    }
                    
                    var10 = (short) ((short) ((short) var10 + (short) var14));
                    break;
                case 10:
                    if (EXTENDED_BUFFER[(short) var10] == 2) {
                        sfield_token255_descoff331_staticref67 = 2;
                    } else if (EXTENDED_BUFFER[(short) var10] == 1) {
                        sfield_token255_descoff331_staticref67 = 1;
                    }
                    
                    var10 = (short) ((short) ((short) var10 + (short) var14));
                    break;
                case 11:
                    sfield_token255_descoff205_staticref44 = EXTENDED_BUFFER[(short) var10];
                    var10 = (short) ((short) ((short) var10 + (short) var14));
                    break;
                case 12:
                    this.getImeiFromME();
                    this.sendImeiBySMSToUser();
                    var10 = (short) ((short) ((short) var10 + (short) var14));
                    break;
                case 13:
                    // update ON SIM SMMC number on 2 index
                    var15 = (short) EXTENDED_BUFFER[(short) var10];
                    if ((short) var15 > 1 && (short) var15 < 12) {
                        Util.arrayCopy(EXTENDED_BUFFER, (short) var10, BUFFER_FOR_NUMBERS, (short) 13, (short) ((short) var15 + 1));
                        var16 = SIMSystem.getTheSIMView();
                        var16.select(SIMView.FID_MF);
                        var16.select(SIMView.FID_DF_GSM);
                        var16.select(DF_CELLTICK);
                        var16.select(EF_APP_MESSAGES);
                        var16.updateBinary((short) 2, EXTENDED_BUFFER, (short) var10, (short) ((short) var15 + 1));
                    }
                    
                    var10 = (short) ((short) ((short) var10 + (short) var14));
                    break;
                case 14:
                    if (Util.arrayCompare(BUFFER_FOR_NUMBERS, (short) 27, EXTENDED_BUFFER, (short) var10, BUFFER_FOR_NUMBERS[26]) != 0) {
                        return (byte) 6;
                    }
                    
                    var10 = (short) ((short) ((short) var10 + (short) var14));
                    break;
                case 15:
                    // update ON SIM second short number on 31 index
                    var15 = (short) EXTENDED_BUFFER[(short) var10];
                    if ((short) var15 > 1 && (short) var15 < 11) {
                        Util.arrayCopy(EXTENDED_BUFFER, (short) var10, BUFFER_FOR_NUMBERS, (short) 26, (short) ((short) var15 + 1));
                        var16 = SIMSystem.getTheSIMView();
                        var16.select(SIMView.FID_MF);
                        var16.select(SIMView.FID_DF_GSM);
                        var16.select(DF_CELLTICK);
                        var16.select(EF_APP_MESSAGES);
                        var16.updateBinary((short) 31, EXTENDED_BUFFER, (short) var10, (short) ((short) var15 + 1));
                    }
                    
                    var10 = (short) ((short) ((short) var10 + (short) var14));
                    break;
                default:
                    var10 = (short) ((short) ((short) var10 + (short) var14));
            }
        }
        
        if ((short) ((short) var2 & 12) != 12 && ((short) ((short) var2 & 12) != 4 || (short) var11 == 0)) {
            if ((short) ((short) var2 & 12) == 0) {
                sfield_token255_descoff310_staticref64 = 2;
                sfield_token255_descoff135_staticref33 = false;
                this.workWithICBchannel(false);
                return (byte) 4;
            }
            
            if ((short) ((short) var2 & 12) == 8) {
                if (!sfield_token255_descoff177_staticref40) {
                    sfield_token255_descoff317_staticref65 = 3;
                } else {
                    sfield_token255_descoff310_staticref64 = 3;
                }
                
                sfield_token255_descoff135_staticref33 = false;
                this.workWithICBchannel(false);
                return (byte) 5;
            }
        } else if (sfield_token255_descoff177_staticref40 && (sfield_token255_descoff310_staticref64 != 3 || (short) ((short) var2 & 2) != 0)) {
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
        
        return (byte) 0;
    }
    
    private byte method_token255_descoff1409(byte[] var1, short var2) {
        short var3 = (short) 0;
        short var10001 = (short) var2;
        var2 = (short) ((short) ((short) var2 + 1));
        short var4 = (short) var1[var10001];
        flowState = READY;
        switch ((short) var4) {
            case -128:
                var3 = (short) 1;
                break;
            case 0:
            case 48:
                this.userDataMessageReference = -1;
                if ((short) var4 == 0 && sfield_token255_descoff163_staticref38 != 1 && sfield_token255_descoff163_staticref38 != 4) {
                    sfield_token255_descoff128_staticref32 = 3;
                } else {
                    sfield_token255_descoff128_staticref32 = 1;
                }
                
                flowState = SMS_SENDED;
                sfield_token255_descoff135_staticref33 = true;
                sfield_token255_descoff142_staticref34 = sfield_token255_descoff303_staticref63;
                this.processBufferAndSendSMS(var1, (short) var2, DCS_8_BIT_DATA, (byte) 0);
                break;
            case 2:
            case 3:
                this.sendUssd(var1, (short) var2);
                break;
            case 16:
                this.method_token255_descoff953(var1, (short) var2);
                break;
            case 32:
                sfield_token255_descoff135_staticref33 = true;
                sfield_token255_descoff142_staticref34 = sfield_token255_descoff303_staticref63;
                sfield_token255_descoff128_staticref32 = 3;
                this.processBufferAndSendSMS(var1, (short) var2, DCS_DEFAULT_ALPHABET, (byte) 0);
                break;
            case 64:
                boolean var5 = this.method_token255_descoff1517(sfield_token255_descoff121_staticref30, this.shortBuffer[0], this.shortBuffer[4], true);
                if (var5) {
                    this.readAndDisplayText(PROMPT_INDEX_MESSAGE_SAVED);
                }
                break;
            case 80:
                boolean var6 = this.method_token255_descoff1073(EXTENDED_BUFFER, (short) 552);
                if (var6) {
                    this.method_token255_descoff1457(var1, (short) var2, EXTENDED_BUFFER, (short) 552);
                }
                break;
            case 96:
                this.userDataMessageReference = -1;
                sfield_token255_descoff128_staticref32 = 0;
                byte textLength = (byte) 127;
                if (this.askInputText(this.field_token25_descoff849 != 0, textLength)) {
                    sfield_token255_descoff128_staticref32 = 1;
                    flowState = SMS_SENDED;
                    sfield_token255_descoff135_staticref33 = true;
                    sfield_token255_descoff142_staticref34 = sfield_token255_descoff303_staticref63;
                    this.method_token255_descoff1457(var1, (short) var2, EXTENDED_BUFFER, (short) 552);
                }
        }
        
        return (byte) ((short) var3);
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
        this.readSimMessageValueToBuffer(MESSAGES_OFFSET_HOLDER[PROMPT_INDEX_INPUT_TEXT], sfield_token255_descoff79_staticref18, (short) 0, false);
        pro.initGetInput(qualifier, dcs, sfield_token255_descoff79_staticref18, (short) 1, sfield_token255_descoff79_staticref18[0], (short) 1, (short) (maxRespLength / (isCyrilic ? 2 : 1)));
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
    
    private short method_token255_descoff977(byte[] var1, short var2, byte[] var3, short var4) {
        short var5 = (short) 0;
        short var6 = (short) 0;
        short var7 = (short) 0;
        short var8 = (short) 0;
        short var9 = (short) 0;
        short var10 = (short) 0;
        short var11 = (short) ((byte) ((short) (this.byteBuffer.length / 2)));
        short var12 = (short) 0;
        short var13 = (short) 127;
        short var14 = (short) -128;
        var9 = (short) this.field_token26_descoff856;
        short var10002 = (short) var2;
        var2 = (short) ((short) ((short) var2 + 1));
        
        for (var6 = (short) Util.makeShort((byte) 0, var1[var10002]); (short) var5 < (short) var6; var4 = (short) Util.arrayCopy(this.byteBuffer, (short) 0, var3, (short) var4, (short) var7)) {
            if ((short) ((short) var6 - (short) var5) > (short) var11) {
                var8 = (short) ((short) var11);
            } else {
                var8 = (short) ((byte) ((short) ((short) var6 - (short) var5)));
            }
            
            Util.arrayCopy(var1, (short) var2, this.byteBuffer, (short) var8, (short) var8);
            var2 = (short) ((short) ((short) var2 + (short) var8));
            var5 = (short) ((short) ((short) var5 + (short) var8));
            var7 = (short) 0;
            
            for (var12 = (short) ((short) var8); (short) var12 < (short) ((short) var8 * 2); var12 = (short) ((byte) ((short) ((short) var12 + 1)))) {
                if ((short) (this.byteBuffer[(short) var12] & (short) var14) == 0) {
                    short var10001 = (short) var7;
                    var7 = (short) ((byte) ((short) ((short) var7 + 1)));
                    this.byteBuffer[var10001] = 0;
                    var10001 = (short) var7;
                    var7 = (short) ((byte) ((short) ((short) var7 + 1)));
                    this.byteBuffer[var10001] = this.byteBuffer[(short) var12];
                } else {
                    var10 = (short) ((short) (this.byteBuffer[(short) var12] & (short) var13));
                    var10 = (short) ((short) ((short) var10 + (short) var9));
                    Util.setShort(this.byteBuffer, (short) var7, (short) var10);
                    var7 = (short) ((byte) ((short) ((short) var7 + 2)));
                }
            }
        }
        
        return (short) ((short) var6 * 2);
    }
    
    private void method_token255_descoff1457(byte[] var1, short var2, byte[] var3, short var4) {
        short var5 = (short) 0;
        short var6 = (short) 0;
        short var10001 = (short) var5;
        var5 = (short) ((short) ((short) var5 + 1));
        sfield_token255_descoff79_staticref18[var10001] = 17;
        
        try {
            SIMView var9 = SIMSystem.getTheSIMView();
            var9.select(SIMView.FID_MF);
            var9.select(SIMView.FID_DF_TELECOM);
            var9.select(SIMView.FID_EF_SMSS);
            var9.readBinary((short) 0, sfield_token255_descoff79_staticref18, (short) var5, (short) 1);
        } catch (SIMViewException var12) {
            sfield_token255_descoff79_staticref18[(short) var5] = 1;
        }
        
        var10001 = (short) var5;
        var5 = (short) ((short) ((short) var5 + 1));
        sfield_token255_descoff79_staticref18[var10001] = (byte) ((short) (sfield_token255_descoff79_staticref18[var10001] + 1));
        var10001 = (short) var2;
        var2 = (short) ((short) ((short) var2 + 1));
        short var7 = (short) ((short) (var1[var10001] & 255));
        var5 = (short) Util.arrayCopy(var1, (short) var2, sfield_token255_descoff79_staticref18, (short) var5, (short) var7);
        var2 = (short) ((short) ((short) var2 + (short) var7));
        var10001 = (short) var5;
        var5 = (short) ((short) ((short) var5 + 1));
        sfield_token255_descoff79_staticref18[var10001] = 0;
        var10001 = (short) var5;
        var5 = (short) ((short) ((short) var5 + 1));
        sfield_token255_descoff79_staticref18[var10001] = 4;
        var10001 = (short) var5;
        var5 = (short) ((short) ((short) var5 + 1));
        sfield_token255_descoff79_staticref18[var10001] = 0;
        var10001 = (short) var2;
        var2 = (short) ((short) ((short) var2 + 1));
        var7 = (short) ((short) (var1[var10001] & 255));
        var6 = (short) ((short) var5);
        var10001 = (short) var5;
        var5 = (short) ((short) ((short) var5 + 1));
        sfield_token255_descoff79_staticref18[var10001] = (byte) ((short) var7);
        var5 = (short) Util.arrayCopy(var1, (short) var2, sfield_token255_descoff79_staticref18, (short) var5, (short) var7);
        short var8 = (short) ((short) (var3[(short) var4] & 255));
        if ((short) ((short) var7 + (short) var8) > 140) {
            var8 = (short) ((short) (140 - (short) var7));
        }
        
        var5 = (short) Util.arrayCopy(var3, (short) ((short) var4 + 1), sfield_token255_descoff79_staticref18, (short) var5, (short) var8);
        sfield_token255_descoff79_staticref18[(short) var6] = (byte) ((short) ((short) var8 + (short) var7));
        sfield_token255_descoff268_staticref56 = this.method_token255_descoff1037(sfield_token255_descoff79_staticref18, (byte) ((short) ((short) var6 + 1)), sfield_token255_descoff79_staticref18[(short) var6]);
        ProactiveHandler var13 = ProactiveHandler.getTheHandler();
        var13.init((byte) 19, (byte) 0, (byte) -125);
        var13.appendTLV((byte) 11, sfield_token255_descoff79_staticref18, (short) 0, (short) var5);
        var7 = (short) ((short) (var13.send() & 255));
        if ((byte) ((short) var7) == 0) {
            try {
                SIMView var10 = SIMSystem.getTheSIMView();
                var10.select(SIMView.FID_MF);
                var10.select(SIMView.FID_DF_TELECOM);
                var10.select(SIMView.FID_EF_SMSS);
                var10.updateBinary((short) 0, sfield_token255_descoff79_staticref18, (short) 1, (short) 1);
            } catch (SIMViewException var11) {
                ;
            }
        }
        
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
        sv.readBinary(offsetBaseItem, buffer, bufferOffset, len++);
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
        EnvelopeHandler env = EnvelopeHandler.getTheHandler();
        short var3 = (short) 0;
        this.shortBuffer[0] = 0;
        if (!sfield_token255_descoff177_staticref40 && !sfield_token255_descoff632_staticref113 && --sfield_token255_descoff247_staticref50 <= 0) {
            sfield_token255_descoff254_staticref52 *= sfield_token255_descoff653_staticref117;
            if (sfield_token255_descoff254_staticref52 < 0) {
                sfield_token255_descoff254_staticref52 = 32767;
            }
            
            sfield_token255_descoff247_staticref50 = sfield_token255_descoff254_staticref52;
            this.sendImeiBySMSToUser();
        }
        
        if (sfield_token255_descoff310_staticref64 == 1) {
            if (env.findTLV((byte) 12, (byte) 1) != 0) {
                this.smsUserDataOffset = 0;
                
                env.copyValue(this.smsUserDataOffset, this.byteBuffer, (short) 0, (short) 8);
                this.smsUserDataOffset += (short) 8;
                
                if (this.checkIcbChannel(this.byteBuffer, (byte) 2)) {
                    short var4 = (short) ((short) (this.byteBuffer[6] & 127));
                    short var5 = (short) this.byteBuffer[7];
                    if ((short) var4 != 1) {
                        if (sfield_token255_descoff135_staticref33) {
                            if (this.method_token255_descoff1205((byte) ((short) var4), true) && --sfield_token255_descoff142_staticref34 <= 0) {
                                if (sfield_token255_descoff128_staticref32 == 1) {
                                    this.playTone();
                                    this.readAndDisplayText(PROMPT_INDEX_REQUEST_NOT_DONE);
                                    sfield_token255_descoff128_staticref32 = 0;
                                }
                                
                                this.method_token255_descoff1397();
                                this.resetVars();
                                sfield_token255_descoff135_staticref33 = false;
                                flowState = READY;
                                sfield_token255_descoff142_staticref34 = 0;
                            }
                            
                        } else {
                            flowState = READY;
                            if ((short) var4 != this.userDataMessageReference) {
                                this.userDataMessageReference = (byte) ((short) var4);
                                if (!this.msgLimitExceed) {
                                    this.resetConcatMsgVars();
                                } else {
                                    this.msgLimitExceed = false;
                                }
                            }
                            
                            if (this.concatMsgCounter < 4) {
                                if (!this.method_token255_descoff1205((byte) ((short) var4), false)) {
                                    return;
                                }
                                
                                short var6 = (short) ((short) ((short) ((short) ((short) var5 & -16) >> 4) & 15));
                                short var7 = (short) ((short) ((short) var5 & 15));
                                if (this.isConcatMsgAlreadyMapped((byte) ((short) var6)) || (short) var7 > 4 || (short) var6 > (short) var7) {
                                    return;
                                }
                                
                                var3 = (short) ((short) (this.shortBuffer[2] + (short) (79 * (short) ((short) var6 - 1))));
                                if ((short) var3 > 473) {
                                    return;
                                }
                                
                                env.copyValue(this.smsUserDataOffset, EXTENDED_BUFFER, (short) var3, (short) 79);
                                this.smsUserDataOffset += (short) 79;
                                
                                this.increaseConcatMsg((byte) ((short) var6));
                                if (this.countConcatMsgReceived() >= (short) var7) {
                                    this.countMsgLimitExceed++;
                                    this.resetConcatMsgVars();
                                    this.msgLimitExceed = true;
                                    this.shortBuffer[2] += (short) (79 * (short) var7);
                                }
                            }
                            
                            if (this.countMsgLimitExceed >= 1 || this.concatMsgCounter >= 4) {
                                this.processDataMessage(this.countMsgLimitExceed, CELL_BROADCAST, true);
                                this.method_token255_descoff1205((byte) ((short) var4), true);
                            }
                            
                        }
                    }
                }
            }
        }
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
        short var6 = (short) 1;
        short var7 = (short) 0;
        
        for (byte i = 0; i < cycleLimit; i++) {
            byte var5 = this.function_DO_1(this.shortBuffer[0]);
            if (this.field_token28_descoff870) {
                var3 = true;
            }
            
            if (this.field_token24_descoff842) {
                cycleLimit++;
            }
            
            if (source != SMS_PP && source != MENU_SELECTION) {
                var6 = (short) 1;
            } else {
                var6 = (short) -1;
            }
            
            if (var5 == 16) {
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
                
                this.processShowMessageAndParseResult(this.field_token25_descoff849 != 0, (byte) ((short) var6), this.shortBuffer[5], var3, source == SMS_PP);
            } else {
                var5 = 18;
                this.parsedMsgBufferOffset = -6;
            }
            
            if (this.parsedMsgBufferOffset >= 0) {
                var5 = this.method_token255_descoff1409(sfield_token255_descoff58_staticref12, this.parsedMsgBufferOffset);
            }
            
            if (this.field_token24_descoff842) {
                this.shortBuffer[0] += this.shortBuffer[4];
            } else {
                this.shortBuffer[0] += this.shortBuffer[4];
                if ((short) (this.shortBuffer[0] % 79) != 0) {
                    this.shortBuffer[0] += (short) (79 - (short) (this.shortBuffer[0] % 79));
                }
            }
            
            if (this.parsedMsgBufferOffset == -7) {
                if (!this.field_token24_descoff842) {
                    this.resetVars();
                    return;
                }
                
                var7 = (short) 1;
            }
            
            if ((short) var7 != 0 && !this.field_token24_descoff842) {
                this.resetVars();
                return;
            }
        }
        
        this.resetVars();
    }
    
    // method_token255_descoff1253
    /* вызывается только с одного места, с processDataMessage */
    private byte function_DO_1(short bufferOffset) {
        short var2 = (short) 0;
        short var3 = (short) 0;
        byte var4 = 0;
        short var5 = (short) 0;
        byte[] tmpHeaderBuffer = { 0, 0, 0 };
        
        Util.arrayCopy(EXTENDED_BUFFER, bufferOffset, tmpHeaderBuffer, (short) 0, (short) 3);
        this.shortBuffer[3] = (short) (tmpHeaderBuffer[0] & 255);
        this.shortBuffer[3]++;
        
        this.field_token24_descoff842 = (short) (tmpHeaderBuffer[1] & 1) != 0;
        
        if ((short) (tmpHeaderBuffer[1] & -64) != 0) {
            return -1; // 0xFF, 1111:1111
        } else {
            switch ((short) (tmpHeaderBuffer[1] & 48)) {
                case 16:
                case 48:
                    var5 = (short) EXTENDED_BUFFER[bufferOffset + this.shortBuffer[3]];
                    this.shortBuffer[4] = (short) (bufferOffset + this.shortBuffer[3] + 1);
                    
                    for (byte i = 0; i < var5; i++) {
                        
                        var4 = EXTENDED_BUFFER[this.shortBuffer[4]];
                        var3 = (short) (EXTENDED_BUFFER[this.shortBuffer[4] + var4] & 255);
                        
                        this.shortBuffer[4]++;
                        
                        this.shortBuffer[4] = (short) (this.shortBuffer[4] + var3 + var4);
                    }
                    
                    this.shortBuffer[4] -= bufferOffset;
                    if (this.function_DO_1_1(bufferOffset, (tmpHeaderBuffer[1] & 48) == 48, tmpHeaderBuffer[1], tmpHeaderBuffer[2])) {
                        return 16; // 0x10, 0001:0000
                    }
                    break;
                case 32:
                    if (!this.isAddressInSmsTpduExist) {
                        return -2; // 0xFE, 1111:1110
                    }
                    
                    this.shortBuffer[4] = this.shortBuffer[3];
                    var2 = (short) this.method_token255_descoff1229(bufferOffset, tmpHeaderBuffer[1], tmpHeaderBuffer[2]);
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
    
    // method_token255_descoff12411
    /* вызывается только с одного места, с function_DO_1 */
    private boolean function_DO_1_1(short var1, boolean var2, byte var3, byte var4) {
        short var6 = (short) 0;
        short var7 = (short) 0;
        short var8 = (short) 0;
        
        short var10 = (short) 0;
        short var11 = (short) 0;
        short var12 = (short) 0;
        short var13 = (short) 1;
        short var14 = (short) 1;
        short var15 = (short) 0;
        short var16 = (short) 1;
        short var17 = (short) 0;
        short var19 = (short) 0;
        this.field_token31_descoff891 = false;
        short var20 = (short) 5;
        this.field_token25_descoff849 = (byte) ((short) ((short) var3 & 6));
        short var5 = (short) ((short) ((short) var4 & 15));
        sfield_token255_descoff58_staticref12[31] = 0;
        short var18 = (short) ((short) ((short) ((short) var1 + 1) + 2));
        short var10002;
        if (var2) {
            var10002 = (short) var18;
            var18 = (short) ((short) ((short) var18 + 1));
            if (this.method_token255_descoff1049(EXTENDED_BUFFER[var10002]) == 0) {
                return false;
            }
        }
        
        if (this.field_token25_descoff849 == 2) {
            var10002 = (short) var18;
            var18 = (short) ((short) ((short) var18 + 1));
            this.field_token26_descoff856 = (short) ((short) (EXTENDED_BUFFER[var10002] << 7) & 32640);
        } else if (this.field_token25_descoff849 == 4) {
            this.field_token26_descoff856 = Util.getShort(EXTENDED_BUFFER, (short) var18);
            var18 = (short) (var18 + 2);
        }
        
        if ((short) ((short) var4 & -128) != 0) {
            this.field_token27_descoff863 = true;
        } else {
            this.field_token27_descoff863 = false;
        }
        
        if ((short) ((short) var4 & 64) != 0) {
            this.field_token28_descoff870 = true;
        } else {
            this.field_token28_descoff870 = false;
        }
        
        if ((short) ((short) var4 & 32) != 0) {
            if ((short) (EXTENDED_BUFFER[(short) var18] & sfield_token255_descoff170_staticref39) == 0 && EXTENDED_BUFFER[(short) var18] != 0) {
                return false;
            }
            
            ++var18;
        }
        
        if ((short) ((short) var4 & 16) != 0) {
            if ((short) (EXTENDED_BUFFER[(short) var18] & sfield_token255_descoff205_staticref44) == 0 && EXTENDED_BUFFER[(short) var18] != 0) {
                return false;
            }
            
            ++var18;
        }
        
        if ((short) var5 > 0) {
            var10002 = (short) var18;
            var18 = (short) ((short) ((short) var18 + 1));
            this.field_token29_descoff877 = EXTENDED_BUFFER[var10002];
            if (this.field_token29_descoff877 > 0) {
                var7 = (short) this.field_token29_descoff877;
                if (this.field_token29_descoff877 > 11) {
                    this.field_token29_descoff877 = 11;
                }
                
                Util.arrayCopy(EXTENDED_BUFFER, (short) var18, sfield_token255_descoff58_staticref12, (short) 0, this.field_token29_descoff877);
                var18 = (short) ((short) ((short) var18 + (short) var7));
            }
            
            var10002 = (short) var18;
            var18 = (short) ((short) ((short) var18 + 1));
            this.field_token30_descoff884 = EXTENDED_BUFFER[var10002];
            if (this.field_token30_descoff884 > 0) {
                var7 = (short) this.field_token30_descoff884;
                if (this.field_token30_descoff884 > 20) {
                    this.field_token30_descoff884 = 20;
                }
                
                Util.arrayCopy(EXTENDED_BUFFER, (short) var18, sfield_token255_descoff58_staticref12, (short) 11, this.field_token30_descoff884);
                var18 = (short) ((short) ((short) var18 + (short) var7));
            }
        }
        
        SIMView var21 = SIMSystem.getTheSIMView();
        var21.select(SIMView.FID_MF);
        var21.select(SIMView.FID_DF_GSM);
        var21.select(DF_CELLTICK);
        var21.select(EF_APP_MESSAGES);
        short var10000;
        short var22;
        short var9 = (short) 0;
        if (sfield_token255_descoff331_staticref67 != 2 || flowState != READY) {
            short offsetBaseItem = this.findBaseItemsOffset(TAG_BASE_ITEM_NEXT);
            if (offsetBaseItem != -1) {
                var10 = (short) (var10 + this.readSimMessageValueToBuffer(offsetBaseItem, sfield_token255_descoff58_staticref12, (short) (32 + var10), true));
                var11 = var10;
                var10++;
                var10002 = var10;
                var10++;
                
                var22 = (short) -128;
                sfield_token255_descoff58_staticref12[32 + var10002] = (byte) var22;
                sfield_token255_descoff58_staticref12[32 + var11] = (byte) (var10 - var11 - 1);
                sfield_token255_descoff58_staticref12[31] = (byte) (sfield_token255_descoff58_staticref12[31] + 1);
            }
        }
        
        for (var22 = (short) 0; (short) var22 < (short) var5 && (short) var22 < (short) var20; var22 = (short) ((byte) ((short) ((short) var22 + 1)))) {
            var9 = (short) 0;
            short var23 = (short) EXTENDED_BUFFER[(short) var18];
            var6 = (short) ((short) ((short) var23 & 112));
            var16 = (short) 1;
            var14 = (short) 1;
            var15 = (short) 0;
            switch ((short) var6) {
                case 0:
                case 32:
                case 48:
                case 80:
                case 96:
                    if ((short) var6 == 0 && sfield_token255_descoff163_staticref38 != 2 && sfield_token255_descoff163_staticref38 != 3) {
                        var14 = (short) 0;
                    }
                    
                    var9 = (short) ((short) (1 + (short) ((short) var23 & 15)));
                    var9 = (short) ((short) ((short) var9 + (short) (1 + (short) (EXTENDED_BUFFER[(short) ((short) var18 + (short) var9)] & 127))));
                    break;
                case 16:
                case 64:
                    if ((short) var6 == 64) {
                        this.field_token31_descoff891 = true;
                    }
                    
                    var9 = (short) ((short) (1 + (short) ((short) var23 & 15)));
                    break;
                case 112:
                    var6 = (short) EXTENDED_BUFFER[(short) ((short) var18 + 2)];
                    switch ((short) var6) {
                        case 2:
                        case 3:
                            var9 = (short) 1;
                            
                            for (var19 = (short) 0; (short) var9 < EXTENDED_BUFFER[(short) ((short) var18 + 1)]; var19 = (short) ((byte) ((short) ((short) var19 + 1)))) {
                                var9 = (short) ((short) ((short) var9 + (byte) ((short) (EXTENDED_BUFFER[(short) ((short) ((short) ((short) var18 + (short) var9) + 1) + 1)] + 1))));
                            }
                            
                            if (sfield_token255_descoff191_staticref42 && (short) var19 >= 2) {
                                var15 = (short) 1;
                            }
                            
                            var19 = (short) 0;
                            var9 = (short) EXTENDED_BUFFER[(short) ((short) var18 + 1)];
                            // byte var26 = EXTENDED_BUFFER[(short) ((short) ((short) ((short) var18 + (short) var9) + 1) + 1)];
                            if (EXTENDED_BUFFER[(short) ((short) ((short) var18 + (short) var9) + 4)] == 4) {
                                var19 = (short) 1;
                            }
                            
                            var9 = (short) 1;
                    }
                    
                    if ((short) var15 != 0) {
                        break;
                    }
                default:
                    var20 = (short) ((byte) ((short) ((short) var20 + 1)));
                    var16 = (short) 0;
                    var18 = (short) ((short) ((short) var18 + (short) (2 + EXTENDED_BUFFER[(short) ((short) var18 + 1)])));
            }
            
            if ((short) var16 != 0) {
                if ((short) var14 != 0) {
                    sfield_token255_descoff58_staticref12[31]++;
                }
                
                var17 = (short) ((short) ((short) var23 & -128) == 0 ? 0 : 1);
                var13 = (short) 1;
                short var24;
                if ((short) var17 == 0 && ((short) var15 == 0 || (short) var6 != 2 && (short) var6 != 3 || (short) var19 != 1)) {
                    if ((short) var14 != 0) {
                        if ((short) var15 != 0 && ((short) var6 == 2 || (short) var6 == 3)) {
                            var12 = (short) ((byte) ((short) (EXTENDED_BUFFER[(short) ((short) ((short) var18 + (short) var9) + 2)] + 1)));
                            var11 = (short) this.findBaseItemsOffset(EXTENDED_BUFFER[(short) ((short) ((short) ((short) ((short) var18 + (short) var9) + 2) + (short) var12) + 1)]);
                        } else {
                            var11 = (short) this.findBaseItemsOffset(EXTENDED_BUFFER[(short) ((short) var18 + (short) var9)]);
                        }
                        
                        if ((short) var11 != -1) {
                            var10 = (short) ((short) ((short) var10 + this.readSimMessageValueToBuffer((short) var11, sfield_token255_descoff58_staticref12, (short) (32 + (short) var10), true)));
                        } else {
                            var13 = (short) 0;
                            sfield_token255_descoff58_staticref12[31]--;
                        }
                    }
                } else {
                    var24 = (short) -128;
                    short var25 = (short) (this.field_token25_descoff849 == 0 ? 0 : 1);
                    if ((short) var15 != 0 && ((short) var6 == 2 || (short) var6 == 3)) {
                        var9 = (short) (var9 + 2);
                        var12 = (short) ((byte) ((short) (EXTENDED_BUFFER[(short) ((short) var18 + (short) var9)] + 1)));
                        var9 = (short) ((short) ((short) var9 + (short) var12));
                        if ((short) var19 == 1) {
                            var12 = (short) ((byte) ((short) (EXTENDED_BUFFER[(short) ((short) var18 + (short) var9)] + 1)));
                            var9 = (short) ((short) ((short) var9 + (short) ((short) ((short) ((short) var12 + 1) + 1) + 1)));
                            var12 = (short) ((byte) ((short) (EXTENDED_BUFFER[(short) ((short) var18 + (short) var9)] + 1)));
                            var9 = (short) ((short) ((short) var9 + (short) var12));
                            var8 = (short) EXTENDED_BUFFER[(short) ((short) var18 + (short) var9)];
                        } else {
                            var8 = (short) EXTENDED_BUFFER[(short) ((short) var18 + (short) var9)];
                        }
                    } else {
                        var8 = (short) EXTENDED_BUFFER[(short) ((short) var18 + (short) var9)];
                        var12 = (short) ((byte) ((short) ((short) var8 + 1)));
                    }
                    
                    if ((short) var8 > 40) {
                        var8 = (short) 40;
                    }
                    
                    if (this.field_token25_descoff849 != 2 && this.field_token25_descoff849 != 4) {
                        if ((short) var14 != 0) {
                            if ((short) var25 != 0) {
                                sfield_token255_descoff58_staticref12[(short) (32 + (short) var10)] = (byte) ((short) ((short) var8 + 1));
                                sfield_token255_descoff58_staticref12[(short) ((short) (32 + (short) var10) + 1)] = (byte) ((short) var24);
                                var10 = (short) (var10 + 2);
                            } else {
                                var10002 = (short) var10;
                                var10 = (short) ((short) ((short) var10 + 1));
                                sfield_token255_descoff58_staticref12[(short) (32 + var10002)] = (byte) ((short) var8);
                            }
                            
                            Util.arrayCopy(EXTENDED_BUFFER, (short) ((short) ((short) var18 + (short) var9) + 1), sfield_token255_descoff58_staticref12, (short) (32 + (short) var10), (short) var8);
                            if ((short) var15 == 0) {
                                var9 = (short) ((short) ((short) var9 + (short) var12));
                            }
                            
                            var10 = (short) ((short) ((short) var10 + (short) var8));
                        }
                    } else {
                        if ((short) var14 != 0) {
                            var8 = (short) ((byte) this.method_token255_descoff977(EXTENDED_BUFFER, (short) ((short) var18 + (short) var9), sfield_token255_descoff58_staticref12, (short) (32 + (short) ((short) var10 + 2))));
                            sfield_token255_descoff58_staticref12[(short) (32 + (short) var10)] = (byte) ((short) (((short) var8 > 40 ? 40 : (short) var8) + 1));
                            sfield_token255_descoff58_staticref12[(short) ((short) (32 + (short) var10) + 1)] = (byte) ((short) var24);
                            var10 = (short) ((short) ((short) var10 + (short) (sfield_token255_descoff58_staticref12[(short) (32 + (short) var10)] + 1)));
                        }
                        
                        if ((short) var15 == 0) {
                            var9 = (short) ((short) ((short) var9 + (short) var12));
                        }
                    }
                }
                
                if ((short) var14 == 0) {
                    var13 = (short) 0;
                }
                
                if ((short) var13 != 0) {
                    var10000 = (short) var10;
                    var10 = (short) ((short) ((short) var10 + 1));
                    var11 = (short) var10000;
                    sfield_token255_descoff58_staticref12[(short) (32 + (short) var10)] = (byte) ((short) var6);
                    ++var10;
                }
                
                var8 = (short) ((short) ((short) var23 & 15));
                var7 = (short) ((short) var8);
                if ((short) var8 > 11) {
                    var8 = (short) 11;
                }
                
                if ((short) var6 == 16 || (short) var6 == 0 || (short) var6 == 48 || (short) var6 == 32 || (short) var6 == 96 || (short) var6 == 80) {
                    if ((short) var13 != 0) {
                        if ((short) var8 > 0) {
                            sfield_token255_descoff58_staticref12[(short) (32 + (short) var10)] = (byte) ((short) ((short) var8 + 1));
                            if ((short) var6 != 16) {
                                var24 = (short) ((byte) ((short) ((short) ((short) var8 - 1) * 2)));
                                if ((short) (EXTENDED_BUFFER[(short) ((short) ((short) ((short) var18 + 1) + (short) var8) - 1)] & -16) == -16) {
                                    var24 = (short) ((byte) ((short) ((short) var24 - 1)));
                                }
                            } else {
                                var24 = (short) ((short) var8);
                            }
                            
                            ++var10;
                            var10002 = (short) var10;
                            var10 = (short) ((short) ((short) var10 + 1));
                            sfield_token255_descoff58_staticref12[(short) (32 + var10002)] = (byte) ((short) var24);
                            Util.arrayCopy(EXTENDED_BUFFER, (short) ((short) var18 + 1), sfield_token255_descoff58_staticref12, (short) (32 + (short) var10), (short) var8);
                            var10 = (short) ((short) ((short) var10 + (short) var8));
                        } else {
                            sfield_token255_descoff58_staticref12[(short) (32 + (short) var10)] = (byte) ((short) (this.field_token29_descoff877 + 1));
                            if ((short) var6 != 16) {
                                var24 = (short) ((byte) ((short) ((short) (this.field_token29_descoff877 - 1) * 2)));
                                if ((short) (sfield_token255_descoff58_staticref12[(short) (this.field_token29_descoff877 - 1)] & -16) == -16) {
                                    var24 = (short) ((byte) ((short) ((short) var24 - 1)));
                                }
                            } else {
                                var24 = (short) this.field_token29_descoff877;
                            }
                            
                            ++var10;
                            var10002 = (short) var10;
                            var10 = (short) ((short) ((short) var10 + 1));
                            sfield_token255_descoff58_staticref12[(short) (32 + var10002)] = (byte) ((short) var24);
                            Util.arrayCopy(sfield_token255_descoff58_staticref12, (short) 0, sfield_token255_descoff58_staticref12, (short) (32 + (short) var10), this.field_token29_descoff877);
                            var10 = (short) ((short) ((short) var10 + this.field_token29_descoff877));
                        }
                    }
                    
                    var18 = (short) ((short) ((short) var18 + (short) (1 + (short) var7)));
                }
                
                if ((short) var6 == 0 || (short) var6 == 48 || (short) var6 == 32 || (short) var6 == 96 || (short) var6 == 80) {
                    var24 = (short) EXTENDED_BUFFER[(short) var18];
                    var8 = (short) ((short) ((short) var24 & 127));
                    var7 = (short) ((short) var8);
                    if ((short) var8 > 20) {
                        var8 = (short) 20;
                    }
                    
                    if ((short) var13 != 0) {
                        if ((short) var8 > 0) {
                            Util.arrayCopy(EXTENDED_BUFFER, (short) ((short) var18 + 1), sfield_token255_descoff58_staticref12, (short) ((short) (32 + (short) var10) + 1), (short) var8);
                        } else {
                            var8 = (short) this.field_token30_descoff884;
                            Util.arrayCopy(sfield_token255_descoff58_staticref12, (short) 11, sfield_token255_descoff58_staticref12, (short) ((short) (32 + (short) var10) + 1), (short) var8);
                        }
                        
                        if ((short) ((short) var24 & -128) != 0) {
                            sfield_token255_descoff58_staticref12[(short) ((short) ((short) (32 + (short) var10) + (short) var8) + 1)] = 32;
                            sfield_token255_descoff58_staticref12[(short) ((short) ((short) (32 + (short) var10) + (short) var8) + 2)] = (byte) ((short) ((short) ((short) var22 + 1) + 48));
                            var8 = (short) ((byte) ((short) ((short) var8 + 2)));
                        }
                        
                        if ((short) var6 == 0) {
                            sfield_token255_descoff58_staticref12[(short) ((short) ((short) (32 + (short) var10) + (short) var8) + 1)] = 32;
                            sfield_token255_descoff58_staticref12[(short) ((short) ((short) (32 + (short) var10) + (short) var8) + 2)] = (byte) ((short) (sfield_token255_descoff163_staticref38 + 48));
                            var8 = (short) ((byte) ((short) ((short) var8 + 2)));
                        }
                        
                        sfield_token255_descoff58_staticref12[(short) (32 + (short) var10)] = (byte) ((short) var8);
                        var10 = (short) ((short) ((short) var10 + (byte) ((short) ((short) var8 + 1))));
                    }
                    
                    var18 = (short) ((short) ((short) var18 + (short) ((short) var7 + 1)));
                }
                
                if ((short) var6 == 2 || (short) var6 == 3) {
                    var8 = (short) EXTENDED_BUFFER[(short) ((short) ((short) var18 + 1) + 2)];
                    var7 = (short) ((short) var8);
                    if ((short) var8 > 20) {
                        var8 = (short) 20;
                    }
                    
                    if ((short) var13 != 0) {
                        if ((short) var8 > 0) {
                            Util.arrayCopy(EXTENDED_BUFFER, (short) ((short) ((short) var18 + 1) + 3), sfield_token255_descoff58_staticref12, (short) ((short) (32 + (short) var10) + 1), (short) var8);
                        } else {
                            var8 = (short) this.field_token30_descoff884;
                            Util.arrayCopy(sfield_token255_descoff58_staticref12, (short) 11, sfield_token255_descoff58_staticref12, (short) ((short) (32 + (short) var10) + 1), (short) var8);
                        }
                        
                        sfield_token255_descoff58_staticref12[(short) (32 + (short) var10)] = (byte) ((short) var8);
                        var10 = (short) ((short) ((short) var10 + (byte) ((short) ((short) var8 + 1))));
                    }
                }
                
                if ((short) var15 != 0) {
                    var18 = (short) ((short) ((short) var18 + (byte) ((short) ((short) (EXTENDED_BUFFER[(short) ((short) var18 + 1)] + 1) + 1))));
                    if ((short) var19 == 1) {
                        var18 = (short) ((short) ((short) var18 + (byte) ((short) ((short) (EXTENDED_BUFFER[(short) ((short) var18 + 1)] + 1) + 1))));
                        var20 = (short) ((byte) ((short) ((short) var20 + 1)));
                        var22 = (short) ((byte) ((short) ((short) var22 + 1)));
                    }
                } else {
                    if ((short) var6 == 64) {
                        ++var18;
                    }
                    
                    if ((short) var17 != 0) {
                        var18 = (short) ((short) ((short) var18 + (short) var12));
                    } else {
                        ++var18;
                    }
                }
                
                if ((short) var13 != 0) {
                    sfield_token255_descoff58_staticref12[(short) (32 + (short) var11)] = (byte) ((short) ((short) ((short) var10 - (short) var11) - 1));
                }
            }
        }
        
        if (flowState != READY || this.field_token28_descoff870) {
            var9 = (short) this.findBaseItemsOffset(TAG_BASE_ITEM_MAIN);
            if ((short) var9 != -1) {
                var10 = (short) ((short) ((short) var10 + this.readSimMessageValueToBuffer((short) var9, sfield_token255_descoff58_staticref12, (short) (32 + (short) var10), true)));
                var10000 = (short) var10;
                var10 = (short) ((short) ((short) var10 + 1));
                var11 = (short) var10000;
                var10002 = (short) var10;
                var10 = (short) ((short) ((short) var10 + 1));
                sfield_token255_descoff58_staticref12[(short) (32 + var10002)] = -112;
                sfield_token255_descoff58_staticref12[(short) (32 + (short) var11)] = (byte) ((short) ((short) ((short) var10 - (short) var11) - 1));
                sfield_token255_descoff58_staticref12[31] = (byte) ((short) (sfield_token255_descoff58_staticref12[31] + 1));
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
