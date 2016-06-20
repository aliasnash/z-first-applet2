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
    
     private static byte flowState = READY;
    
    private static final byte   TAG_BASE_ITEM_NEXT                      = 0x64;
    private static final byte   TAG_BASE_ITEM_MAIN                      = 0x65;
    private static final byte   TAG_BASE_ITEM_CALL                      = 0x01;
    private static final byte   TAG_BASE_ITEM_DETAIL                    = 0x02;
    private static final byte   TAG_BASE_ITEM_YES                       = 0x03;
    private static final byte   TAG_BASE_ITEM_NO                        = 0x04;
    
    // field_token28_descoff870
    private boolean             showTextFromMessage;
    private boolean             playSoundFromMessage;
    private static boolean      isAllowPlaySound                        = true;
    
    public short                smsUserDataOffset;
    private boolean             isAddressInSmsTpduExist                 = false;
    private byte[]              byteBuffer;
    public short[]              shortBuffer;
    public short                messageLength;
    public short                severalMessageOffset;
    private boolean[]           concatMsgMapper;
    public boolean              serviceInProgress;
    private byte                userDataMessageReference                = -1;
    public boolean              msgLimitExceed;
    private byte                firstByteMarkerClass                    = 0;
    private byte                concatMsgCounter;
    public byte                 countMsgLimitExceed                     = 0;
    
    public short                messageBlockOffset;
    
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
    // field_token15_descoff779
    public boolean              isAllMessagesShowed                     = false;
    // field_token26_descoff856
    private short               unicodePattern                          = 0;
    
    /* WARNING START */
    // private static byte sfield_token255_descoff170_staticref39;
    private static byte         techSMSaccessSmsAvailability            = (byte) 0xFF;
    // private static byte sfield_token255_descoff205_staticref44;
    private static byte         techSMSaccessIcbAvailability            = (byte) 0xFF;
    /* WARNING END */
    
    // private static boolean sfield_token255_descoff135_staticref33;
    private static boolean      inProgressOnlyForICB;
    // private static short sfield_token255_descoff142_staticref34;
    private static short        checkerOnlyForICB;
    
    // private static byte sfield_token255_descoff303_staticref63 = 4;
    private static byte         techSMSvalue1                           = 4;
    // private static byte sfield_token255_descoff331_staticref67 = 1;
    private static byte         techSMSvalue2                           = 1;
    
    /* UNKNOWN VARS */
    
    // public boolean[] field_token15_descoff779;
    
    private static short        sfield_token255_descoff653_staticref117 = 2;
    
    private static byte         sfield_token255_descoff268_staticref56;
    
    private static boolean      sfield_token255_descoff191_staticref42;
    
    private static boolean      sfield_token255_descoff639_staticref114 = true;
    public final byte           field_token2_descoff688                 = 6;
    
    private static final byte   CODING_0_LATIN                          = 0;
    private static final byte   CODING_2_UNICODE                        = 2;
    private static final byte   CODING_4_UNICODE                        = 4;
    private static final byte   CODING_6_UNICODE                        = 6;
    
    private byte                dataCoding                              = 0;
    // private byte field_token25_descoff849 = 0;
    
    public final byte           field_token7_descoff723                 = 1;
    public final byte           field_token14_descoff772                = 5;
    private static byte         sfield_token255_descoff128_staticref32;
    private static boolean      sfield_token255_descoff177_staticref40;
    private static byte         sfield_token255_descoff212_staticref45;
    public final byte           field_token5_descoff709                 = 2;
    public final byte           field_token12_descoff758                = 3;
    private static boolean      sfield_token255_descoff632_staticref113 = true;
    public final byte           field_token1_descoff681                 = 3;
    
    private boolean             typeMessageC                            = false;
    // private boolean field_token31_descoff891 = false;
    private static short        sfield_token255_descoff247_staticref50;
    
    private static short        sfield_token255_descoff660_staticref119 = 1;
    private static short        sfield_token255_descoff667_staticref121 = 1;
    
    public final byte           field_token4_descoff702                 = 1;
    public final byte           field_token11_descoff751                = 2;
    
    public final byte           field_token9_descoff737                 = 0;
    public byte[]               field_token16_descoff786;
    
    private static byte         sfield_token255_descoff310_staticref64  = 2;
    private static byte         sfield_token255_descoff317_staticref65  = 1;
    
    private static short        sfield_token255_descoff646_staticref115 = 4;
    public final byte           field_token3_descoff695                 = 0;
    
    public final byte           field_token8_descoff730                 = 2;
    
    private static boolean      sfield_token255_descoff184_staticref41;
    
    public final byte           field_token6_descoff716                 = 0;
    public final byte           field_token13_descoff765                = 4;
    
    private static short        sfield_token255_descoff254_staticref52;
    
    public final byte           field_token0_descoff674                 = 3;
    
    private static final short  DISPLAY_TEXT_OFFSET_NO_ITEMS            = -2;
    private static final short  DISPLAY_TEXT_OFFSET_ERROR               = -7;
    private static final short  DISPLAY_TEXT_OFFSET_NO_RESPONSE         = -4;
    private static final short  DISPLAY_TEXT_OFFSET_HOME                = -6;
    
    private short               parsedMsgBufferOffset                   = DISPLAY_TEXT_OFFSET_HOME;
    
    private static byte         sfield_token255_descoff163_staticref38;
    public final byte           field_token10_descoff744                = 1;
    // private static boolean sfield_token255_descoff149_staticref36;
    
    public StkAppletSmsIcb() {
        ToolkitRegistry reg = ToolkitRegistry.getEntry();
        
        try {
            this.byteBuffer = JCSystem.makeTransientByteArray((short) 41, JCSystem.CLEAR_ON_RESET);
            this.concatMsgMapper = JCSystem.makeTransientBooleanArray((short) 4, JCSystem.CLEAR_ON_RESET);
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
            inProgressOnlyForICB = false;
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
        inProgressOnlyForICB = false;
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
        // 0: isCyrilic=true, EXTENDED_BUFFER, bufferOffset=556, maxLength=34
        
        short len = 0;
        short msgOffset = 601;
        
        if (isCyrilic) {
            PARSED_MESSAGE[msgOffset++] = (byte) 0x80;
            // 0: msgOffset =602
            
            for (byte i = 0; i < 20; i = (byte) (i + 2)) {
                PARSED_MESSAGE[msgOffset + i] = 0x00;
                PARSED_MESSAGE[msgOffset + i + 1] = 0x20;
            }
            
            len = (short) (maxLength < 20 ? maxLength : 20);
            // 0: len = 20
        } else {
            for (byte i = 0; i < 21; i++) {
                PARSED_MESSAGE[msgOffset + i] = 0x20;
            }
            
            len = (short) (maxLength < 10 ? maxLength : 10);
        }
        
        PARSED_MESSAGE[600] = (byte) (len + (isCyrilic ? 1 : 0));
        // 0: PA[600] = 20 + 1 = 21
        Util.arrayCopy(buffer, bufferOffset, PARSED_MESSAGE, msgOffset, len);
        // 0: EX[556] copy PA[602] len=20
    }
    
    // method_token255_descoff1013
    private byte displayText(byte dcs, byte[] text, short offset, short length) {
        // dcs = 8, offset = 556, length = 34
        
        ProactiveHandler proHandlr = ProactiveHandler.getTheHandler();
        proHandlr.initDisplayText((byte) 0x80, dcs, text, offset, length);
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
        short var4 = (short) (var1 == TMP_BUFFER_1 ? sfield_token255_descoff660_staticref119 : sfield_token255_descoff667_staticref121);
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
            if (var1 == TMP_BUFFER_1) {
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
        
        short len = (short) (buffer[index++] & 0xFF); // index=86, len=4
        
        // 1. first 1 byte that inform the length of address
        // 2. 2nd 1 byte that inform TON/NPI
        // 3. the rest is address in BCD swapped format
        offset = Util.arrayCopy(buffer, index, tmpMessageBuffer, offset, len); // TP-DA
        index = (short) (index + len); // index=91
        
        tmpMessageBuffer[offset++] = 0x00; // TP-PID
        tmpMessageBuffer[offset++] = dcsValue; // TP-DCS
        tmpMessageBuffer[offset++] = validatePeriod; // TP-VP
        
        len = buffer[index++]; // index=91, len=9
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
    
    // method_token255_descoff1025
    /* вызывается только с одного места, с processDataMessage */
    private void processShowMessageAndParseResult(boolean isCyrilic, short offset, boolean showText, byte source) {
        // isCyrilic=true, offset=552, showText=true, source=SMS
        
        short maxLength = 0;
        short var10 = 0;
        byte displayTextRes = 0;
        this.parsedMsgBufferOffset = DISPLAY_TEXT_OFFSET_HOME;
        
        byte dataCoding = (byte) (isCyrilic ? 8 : 4);
        // dataCoding = 8
        byte limit = (byte) (source != SMS_PP && source != MENU_SELECTION ? 1 : -1); // limit = 1 ICB / -1 SMS
        // limit = -1
        
        boolean isSmsSource = (source == SMS_PP);
        // isSmsSource = true
        
        byte msgParts = EXTENDED_BUFFER[offset];
        // msgParts = 1
        
        if (msgParts >= 1) {
            for (byte i = 0; i != limit; i++) {
                // 0: limit = -1
                if (i == 127) {
                    i = 0;
                }
                
                var10 = 0;
                
                offset++;
                // 0: offset = 553
                
                for (byte j = 0; j < msgParts; j++) {
                    this.isAllMessagesShowed = (j == (msgParts - 1));
                    // isAllMessagesShowed = true
                    
                    // msgLen = EX[553] = 2
                    byte msgLen = EXTENDED_BUFFER[offset++];
                    // offset = 554
                    
                    Util.arrayCopy(EXTENDED_BUFFER, offset, this.byteBuffer, (short) 0, (short) msgLen);
                    // EX[554] copy BB[0] len 2
                    
                    // dataCoding = headerSecondByte & 0x06 = 2
                    if (this.dataCoding != CODING_2_UNICODE && this.dataCoding != CODING_4_UNICODE) {
                        maxLength = Util.makeShort((byte) 0, this.byteBuffer[msgLen - 1]);
                    } else {
                        maxLength = Util.getShort(PARSED_MESSAGE, (short) (622 + (var10 * 2)));
                        // 0: maxLength = PA[662+ 0*2] = PA[662] = 34
                        var10++;
                        // 0: var10 = 1
                    }
                    
                    offset = (short) (offset + msgLen);
                    // offset = 554 + 2 = 556
                    
                    if (isSmsSource || this.typeMessageC) {
                        this.copyBufferToParsedMessage(isCyrilic, EXTENDED_BUFFER, offset, maxLength);
                        this.typeMessageC = false;
                        
                        if (isSmsSource) {
                            // severalMessageOffset = 0
                            // messageLength = 81
                            this.method_token255_descoff1517(TMP_BUFFER_1, this.severalMessageOffset, this.messageLength, false);
                        }
                    }
                    
                    if (!showText) {
                        return;
                    }
                    
                    isSmsSource = false;
                    if (isCyrilic) {
                        // dataCoding = 8, offset = 556, maxLength = 34
                        displayTextRes = this.displayText(dataCoding, EXTENDED_BUFFER, offset, maxLength > 159 ? 158 : maxLength);
                    } else {
                        displayTextRes = this.displayText(dataCoding, EXTENDED_BUFFER, offset, maxLength > 159 ? 159 : maxLength);
                    }
                    
                    if (displayTextRes != RES_CMD_PERF_NO_RESP_FROM_USER) {
                        this.processResultOfDisplayText(displayTextRes);
                        if (this.parsedMsgBufferOffset != DISPLAY_TEXT_OFFSET_HOME) {
                            return;
                        }
                    } else {
                        j = msgParts;
                        this.parsedMsgBufferOffset = DISPLAY_TEXT_OFFSET_NO_RESPONSE;
                    }
                    
                    offset = (short) (offset + maxLength);
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
                this.parsedMsgBufferOffset = this.findParsedMessageIndex();
                
                if (this.parsedMsgBufferOffset != DISPLAY_TEXT_OFFSET_ERROR) {
                    
                    if (this.parsedMsgBufferOffset != DISPLAY_TEXT_OFFSET_HOME && this.parsedMsgBufferOffset < 0) {
                        this.parsedMsgBufferOffset = DISPLAY_TEXT_OFFSET_NO_RESPONSE;
                    }
                    
                } else {
                    inProgressOnlyForICB = true;
                    flowState = READY;
                    checkerOnlyForICB = techSMSvalue1;
                    this.parsedMsgBufferOffset = DISPLAY_TEXT_OFFSET_ERROR;
                }
                
                break;
            case RES_CMD_PERF_SESSION_TERM_USER:
            case RES_CMD_PERF_BACKWARD_MOVE_REQ:
                inProgressOnlyForICB = true;
                flowState = READY;
                checkerOnlyForICB = techSMSvalue1;
                this.parsedMsgBufferOffset = DISPLAY_TEXT_OFFSET_ERROR;
                break;
            default:
                inProgressOnlyForICB = true;
                checkerOnlyForICB = techSMSvalue1;
                this.parsedMsgBufferOffset = DISPLAY_TEXT_OFFSET_ERROR;
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
    private boolean getInputNumber(byte[] buffer, short bufferOffset) {
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
    
    // method_token255_descoff953
    private void sendCall(byte[] buffer, short bufferOffset) {
        ProactiveHandler pro = ProactiveHandler.getTheHandler();
        pro.init(PRO_CMD_SET_UP_CALL, (byte) 0, DEV_ID_NETWORK);
        short len = buffer[bufferOffset++];
        pro.appendTLV(TAG_ADDRESS, buffer, (short) (bufferOffset + 1), (short) (len - 1));
        pro.send();
    }
    
    private byte method_token255_descoff1037(byte[] buffer, short offset, byte len) {
        short result = 0;
        
        for (short i = 0; i < len; i++) {
            short value = buffer[offset + i];
            
            for (short j = 0; j < 8; j++) {
                if ((value & 1) != 0) {
                    value = (short) ((value >> 1) ^ 93);
                } else {
                    value = (short) (value >> 1);
                }
            }
            
            result = (short) (result ^ value);
        }
        
        result = (short) (result | -128);
        if ((result & -16) == -128) {
            result = (short) (result | 16);
        }
        
        return (byte) result;
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
    
    // method_token255_descoff917
    private short findParsedMessageIndex() {
        short parsedOffset = 31;
        
        if (!this.isAllMessagesShowed) {
            return DISPLAY_TEXT_OFFSET_HOME;
        } else {
            short itemCount = PARSED_MESSAGE[parsedOffset];
            if (itemCount <= 0) {
                return DISPLAY_TEXT_OFFSET_NO_ITEMS;
            } else {
                ProactiveHandler pro = ProactiveHandler.getTheHandler();
                pro.init(PRO_CMD_SELECT_ITEM, (byte) 0x00, DEV_ID_ME);
                
                short offset = (short) (parsedOffset + 1);
                // offset = 32
                
                if (techSMSvalue2 == 2 && flowState == READY && itemCount == 1) {
                    return (short) (offset + PARSED_MESSAGE[offset] + 2);
                } else {
                    for (byte i = 0; i < itemCount; i++) {
                        // byte v = PARSED_MESSAGE[offset + PARSED_MESSAGE[offset] + 2];
                        // if (flowState != 4 || v == -112) {
                        pro.appendTLV(TAG_ITEM, (byte) (i + 1), PARSED_MESSAGE, (short) (offset + 1), PARSED_MESSAGE[offset]);
                        // 0: tagValue = 1, PA[33], len=PA[32]=20 - позвонить
                        // 1: tagValue = 2, PA[63], len=PA[62]=21 - ???text
                        // 2: tagValue = 3, PA[102], len=PA[101]=20 - ???text
                        
                        offset += (short) (PARSED_MESSAGE[offset] + 1);
                        // 0: offset = 32 + PA[32] + 1 = 32+20+1 = 53
                        // 1: offset = 62 + PA[62] + 1 = 62+21+1 = 84
                        // 2: offset = 101 + PA[101] + 1 = 101+20+1 = 122
                        
                        offset += (short) (PARSED_MESSAGE[offset] + 1);
                        // 0: offset = 53 + PA[53] + 1 = 53+8+1 = 62
                        // 1: offset = 84 + PA[84] + 1 = 84+16+1 = 101
                        // 2: offset = 122 + PA[122] + 1 = 122+15+1 = 138
                    }
                    
                    byte result = pro.send();
                    if (result == RES_CMD_PERF_SESSION_TERM_USER) {
                        return DISPLAY_TEXT_OFFSET_ERROR;
                    } else if (result == RES_CMD_PERF_BACKWARD_MOVE_REQ) {
                        return DISPLAY_TEXT_OFFSET_ERROR;
                    } else if (result == RES_CMD_PERF_NO_RESP_FROM_USER) {
                        return DISPLAY_TEXT_OFFSET_NO_RESPONSE;
                    } else if (result != RES_CMD_PERF) {
                        return DISPLAY_TEXT_OFFSET_ERROR;
                    } else {
                        ProactiveResponseHandler proResp = ProactiveResponseHandler.getTheHandler();
                        byte itemIdentifier = (byte) (proResp.getItemIdentifier() - 1);
                        if (itemIdentifier < 0) {
                            return DISPLAY_TEXT_OFFSET_ERROR;
                        } else {
                            // parsedOffset = 31
                            offset = (short) (parsedOffset + 1);
                            // offset = 32
                            
                            offset += (short) (PARSED_MESSAGE[offset] + 1);
                            // offset = 32 + PA[32] + 1 = 32+20+1 = 53
                            
                            for (byte j = 0; j < itemIdentifier; j++) {
                                offset += (short) (PARSED_MESSAGE[offset] + 1);
                                offset += (short) (PARSED_MESSAGE[offset] + 1);
                            }
                            
                            if (PARSED_MESSAGE[offset + 1] == 0x90) { // 0x90 = -112
                                return DISPLAY_TEXT_OFFSET_HOME;
                            } else {
                                // return PA[54] = 0x30
                                return (short) (offset + 1);
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
    
    // method_token255_descoff965
    private void workWithBuffers(byte[] bufferSrc, short offsetSrc, byte[] bufferDst, short offsetDst) {
        // EXTENDED_BUFFER, offsetSrc=60, EXTENDED_BUFFER, offsetDst=552
        // offsetSrc = 60
        // offsetDst = 552
        
        short lenOfMessage = 0;
        short var9 = (bufferDst[offsetDst++] = bufferSrc[offsetSrc++]);
        // var9 = 1
        // offsetSrc = 61
        // offsetDst = 553
        
        for (short i = 0; i < var9; i++) {
            byte length = bufferSrc[offsetSrc++];
            // length = 2
            // offsetSrc = 62
            
            Util.arrayCopy(bufferSrc, offsetSrc, this.byteBuffer, (short) 0, length);
            
            byte savedFirstByte = this.byteBuffer[0];
            // savedFirstByte = 0
            
            short savedLastByte = (short) (this.byteBuffer[length - 1] & 255);
            // savedLastByte = 0x11 = 17
            
            bufferDst[offsetDst++] = length;
            // offsetDst = 554
            
            offsetSrc = (short) (offsetSrc + length - 1);
            // offsetSrc = 62 + 2 -1 = 63
            
            lenOfMessage = this.allocateStringToBuffer(bufferSrc, offsetSrc, bufferDst, (short) (offsetDst + length));
            // lenOfMessage = 34
            
            Util.setShort(PARSED_MESSAGE, (short) (622 + (i * 2)), lenOfMessage);
            // PA[622] = 34
            
            offsetSrc = (short) (offsetSrc + savedLastByte + 1);
            // offsetSrc = 106 + 63 + 17 = 186
            
            this.byteBuffer[0] = savedFirstByte;
            // BB[0] = 0
            this.byteBuffer[length - 1] = (byte) lenOfMessage;
            // BB[1] = 34
            Util.arrayCopy(this.byteBuffer, (short) 0, bufferDst, offsetDst, length);
            offsetDst = (short) (offsetDst + length + lenOfMessage);
            // offsetDst = 554 + 2 + 34 = 590
        }
        
    }
    
    // method_token255_descoff1445
    private boolean sendImeiBySMSToUser() {
        this.buildImeiAndVersionBuffer();
        
        if (this.processBufferAndSendSMS(EXTENDED_BUFFER, (short) 0, DCS_8_BIT_DATA, (byte) -76)) {
            inProgressOnlyForICB = true;
            checkerOnlyForICB = techSMSvalue1;
            imeiFlag = false;
            return true;
        } else {
            return false;
        }
    }
    
    private boolean method_token255_descoff1517(byte[] buffer, short exOffset, short msgLen, boolean var4) {
        // var1 = sfield_token255_descoff114_staticref28, exOffset = 0, msgLen = 81, var4 = false
        
        short var5 = (buffer == TMP_BUFFER_1 ? sfield_token255_descoff660_staticref119 : sfield_token255_descoff667_staticref121);
        // var5 = 1
        
        short messagePartLen = PARSED_MESSAGE[600];
        // messagePartLen = PA[600] = 21
        
        short var7 = (short) (msgLen + messagePartLen + 1 + 2 - 577 - var5);
        // var7 = 81 + 21 + 1 + 2 - 557 - 1 = -453
        
        if (var7 > 0) {
            if (var4 && this.readAndDisplayText(PROMPT_INDEX_REMOVE_MESSAGES) != 0) {
                return false;
            }
            
            if (!this.method_token255_descoff989(buffer, var7, false)) {
                return false;
            }
            
            var5 = (buffer == TMP_BUFFER_1 ? sfield_token255_descoff660_staticref119 : sfield_token255_descoff667_staticref121);
        }
        
        while ((9 + (24 * (buffer[0] + 1))) > 255) {
            if (!this.method_token255_descoff989(buffer, (short) 2, false)) {
                return false;
            }
            
            var5 = (buffer == TMP_BUFFER_1 ? sfield_token255_descoff660_staticref119 : sfield_token255_descoff667_staticref121);
        }
        
        Util.arrayCopy(PARSED_MESSAGE, (short) 600, buffer, var5, (short) (messagePartLen + 1));
        // PA[600] copy var1[1] len 22
        
        Util.setShort(buffer, (short) (var5 + messagePartLen + 1), msgLen);
        // var1[1+21+1] -> 81
        
        Util.arrayCopyNonAtomic(EXTENDED_BUFFER, exOffset, buffer, (short) (var5 + messagePartLen + 1 + 2), msgLen);
        // EX[0] copy var1[1+21+1+2] len 81 (full message incoming)
        
        var5 = (short) (var5 + messagePartLen + msgLen + 1 + 2);
        // var5 = 1 + 21 + 81 + 1 + 2 = 106
        
        if (buffer == TMP_BUFFER_1) {
            sfield_token255_descoff660_staticref119 = var5;
            // sfield_token255_descoff660_staticref119 = 106
        } else {
            sfield_token255_descoff667_staticref121 = var5;
        }
        
        buffer[0]++;
        return true;
    }
    
    // method_token255_descoff1229
    /* вызывается только с одного места, с function_DO_1 */
    private byte processTechnicalMessage(short bufferOffset, byte headerSecondByte, byte headerThirdByte) {
        short var4 = 0;
        short var5 = 0;
        short var6 = 0;
        short var7 = 0;
        short var8 = 0;
        byte messagePartCount = (byte) (headerThirdByte & 0x0F); // будет 4 если байт = 14 из примера
        short var11 = 0; // (short) (sfield_token255_descoff149_staticref36 ? 1 : 0);
        short offset = (short) (bufferOffset + 1 + 2);
        // offset = 3
        
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
                    // sfield_token255_descoff149_staticref36 = false;
                    sfield_token255_descoff163_staticref38 = EXTENDED_BUFFER[offset + 5];
                    techSMSaccessSmsAvailability = EXTENDED_BUFFER[offset + 6];
                    sfield_token255_descoff639_staticref114 = (EXTENDED_BUFFER[offset + 7] & -128) != 0;
                    sfield_token255_descoff177_staticref40 = (EXTENDED_BUFFER[offset + 7] & 64) != 0;
                    sfield_token255_descoff184_staticref41 = (EXTENDED_BUFFER[offset + 7] & 32) != 0;
                    sfield_token255_descoff191_staticref42 = (EXTENDED_BUFFER[offset + 7] & 16) != 0;
                    if (!sfield_token255_descoff177_staticref40) {
                        sfield_token255_descoff317_staticref65 = sfield_token255_descoff310_staticref64;
                        sfield_token255_descoff310_staticref64 = 2;
                        sfield_token255_descoff247_staticref50 = sfield_token255_descoff254_staticref52 = sfield_token255_descoff646_staticref115;
                        inProgressOnlyForICB = false;
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
                    techSMSvalue1 = EXTENDED_BUFFER[offset + 2];
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
                        techSMSvalue2 = 2;
                    } else if (EXTENDED_BUFFER[offset] == 1) {
                        techSMSvalue2 = 1;
                    }
                    
                    offset = (short) (offset + nextBufferValue);
                    break;
                case 11:
                    techSMSaccessIcbAvailability = EXTENDED_BUFFER[offset];
                    offset = (short) (offset + nextBufferValue);
                    break;
                case 12:
                    this.getImeiFromME();
                    this.sendImeiBySMSToUser();
                    offset = (short) (offset + nextBufferValue);
                    break;
                case 13:
                    // update ON SIM SMSC number on 2 index
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
                inProgressOnlyForICB = false;
                this.workWithICBchannel(false);
                return 4;
            }
            
            if ((headerSecondByte & 0x0C) == 0x08) {
                if (!sfield_token255_descoff177_staticref40) {
                    sfield_token255_descoff317_staticref65 = 3;
                } else {
                    sfield_token255_descoff310_staticref64 = 3;
                }
                
                inProgressOnlyForICB = false;
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
    private byte doCommandFromBuffer(byte[] buffer, short offset) {
        byte result = 0;
        
        // 0: offset = 54, value = 0x10
        // 1: offset = 85, value = 0x20
        // 2: offset = 123, value = 0x30
        short value = buffer[offset++];
        // 0: offset = 55
        // 1: offset = 86
        // 2: offset = 124
        
        flowState = READY;
        switch (value) {
            case 0x80:
                result = 1;
                break;
            case 0x00:
            case 0x30:
                this.userDataMessageReference = -1;
                if (value == 0 && sfield_token255_descoff163_staticref38 != 1 && sfield_token255_descoff163_staticref38 != 4) {
                    // sfield_token255_descoff128_staticref32 = 3;
                } else {
                    sfield_token255_descoff128_staticref32 = 1;
                }
                
                flowState = SMS_SENDED;
                inProgressOnlyForICB = true;
                checkerOnlyForICB = techSMSvalue1;
                
                this.processBufferAndSendSMS(buffer, offset, DCS_8_BIT_DATA, (byte) 0);
                break;
            case 0x02:
            case 0x03:
                this.sendUssd(buffer, offset);
                break;
            case 0x10:
                this.sendCall(buffer, offset);
                break;
            case 0x20:
                inProgressOnlyForICB = true;
                checkerOnlyForICB = techSMSvalue1;
                // sfield_token255_descoff128_staticref32 = 3;
                
                this.processBufferAndSendSMS(buffer, offset, DCS_DEFAULT_ALPHABET, (byte) 0);
                break;
            case 0x40:
                boolean var5 = this.method_token255_descoff1517(sfield_token255_descoff121_staticref30, this.severalMessageOffset, this.messageLength, true);
                if (var5) {
                    this.readAndDisplayText(PROMPT_INDEX_MESSAGE_SAVED);
                }
                break;
            case 0x50:
                boolean var6 = this.getInputNumber(EXTENDED_BUFFER, (short) 552);
                if (var6) {
                    this.composeAndSendSMS(buffer, offset, EXTENDED_BUFFER, (short) 552);
                }
                break;
            case 0x60:
                this.userDataMessageReference = -1;
                sfield_token255_descoff128_staticref32 = 0;
                byte textLength = (byte) 127;
                if (this.getInputText(this.dataCoding != CODING_0_LATIN, textLength)) {
                    sfield_token255_descoff128_staticref32 = 1;
                    flowState = SMS_SENDED;
                    inProgressOnlyForICB = true;
                    checkerOnlyForICB = techSMSvalue1;
                    this.composeAndSendSMS(buffer, offset, EXTENDED_BUFFER, (short) 552);
                }
        }
        
        return result;
    }
    
    // method_token255_descoff1085
    private boolean getInputText(boolean isCyrilic, byte maxRespLength) {
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
    
    // method_token255_descoff977
    private short allocateStringToBuffer(byte[] buffer, short bufferOffset, byte[] bufferDst, short bufferDstOffset) {
        // 2: EXTENDED_BUFFER, bufferOffset = 38, PARSED_MESSAGE, bufferDstOffset = 64
        // end: EXTENDED_BUFFER, bufferOffset = 63, EXTENDED_BUFFER, bufferDstOffset = 556
        
        // unicodePattern = EX[4] = 0x0B << 7 = 0x0580 & 0x7F80 = 0x0580
        
        short byteBufferOffset = 0;
        short maxLen = 0;
        short unicode = this.unicodePattern;
        short letter = 0;
        short byteBufferLen = (short) (this.byteBuffer.length / 2); // 41 / 2
        
        // 2: bufferOffset = 38
        // end: bufferOffset = 63
        short len = Util.makeShort((byte) 0, buffer[bufferOffset++]);
        // 2: len = 0x000A = 10, bufferOffset = 39
        // end: len=0x0011 = 17, bufferOffset = 64
        
        for (short i = 0; i < len;) {
            
            if (len - i > byteBufferLen) {
                maxLen = byteBufferLen;
            } else {
                maxLen = (short) (len - i);
                // 2-0: maxLen = 10
                // end-0: maxLen = 17
            }
            
            Util.arrayCopy(buffer, bufferOffset, this.byteBuffer, maxLen, maxLen);
            // 2-0: EX[39] copy BB[10] len 10 - все сообщение
            // last-0: EX[64] copy BB[17] len 17 - все сообщение
            
            bufferOffset = (short) (bufferOffset + maxLen);
            // 2-0: bufferOffset = 39+10=49
            // end-0: bufferOffset = 64+17=81
            
            i = (short) (i + maxLen);
            // 2-0: i=10
            // end-0: i=17
            
            byteBufferOffset = 0;
            
            // 2-0: from j=10 to j<20
            // end-0: from j=17 to j<34
            for (short j = maxLen; j < (short) (maxLen * 2); j++) {
                
                // если буква русская, т.е. 7й бит = 1
                if ((this.byteBuffer[j] & 0x80) == 0) { // символ ASCII, добавляем 0 в старший байт
                    this.byteBuffer[byteBufferOffset++] = 0;
                    this.byteBuffer[byteBufferOffset++] = this.byteBuffer[j];
                } else { // кириллица, делаем шорт из байта
                    letter = (short) (this.byteBuffer[j] & 0x7F);
                    letter = (short) (letter + unicode); // 0x0580 + 12 = 0x0412 = В
                    Util.setShort(this.byteBuffer, byteBufferOffset, letter);
                    byteBufferOffset = (short) (byteBufferOffset + 2);
                }
            }
            
            bufferDstOffset = Util.arrayCopy(this.byteBuffer, (short) 0, bufferDst, bufferDstOffset, byteBufferOffset);
        }
        
        // end: return 17*2=34
        return (short) (len * 2);
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
        // 1: len = 0x13 = 19
        len++;
        // 1: len = 20
        sv.readBinary(offsetBaseItem, buffer, bufferOffset, len);
        len++;
        // 1: len = 21
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
        this.severalMessageOffset = 0;
        
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
                        if (inProgressOnlyForICB) {
                            
                            boolean exist = this.isIcbMsgRefExist(icbUserDataFirstByte, true);
                            
                            if (!exist && --checkerOnlyForICB <= 0) {
                                if (sfield_token255_descoff128_staticref32 == 1) {
                                    this.playTone();
                                    this.readAndDisplayText(PROMPT_INDEX_REQUEST_NOT_DONE);
                                    sfield_token255_descoff128_staticref32 = 0;
                                }
                                
                                this.resetIcbMsgRefsHolder();
                                this.resetVars();
                                inProgressOnlyForICB = false;
                                flowState = READY;
                                checkerOnlyForICB = 0;
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
                this.severalMessageOffset = 0;
                this.userDataMessageReference = -1;
                sfield_token255_descoff128_staticref32 = 0;
                inProgressOnlyForICB = false;
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
    private void processDataMessage(byte cycleLimit, byte source, boolean showResultText) {
        // cycleLimit = 1;
        // processCellBroadcastPage: var2 =1 ; var3 = true; - CELL_BROADCAST
        // eventSmsPPDataDownload: var2 = 2; var3 = false; - SMS_PP
        // eventMenuSelection: var2 = 3; var3 = true; - MENU_SELECTION
        
        boolean flag = false;
        
        for (byte i = 0; i < cycleLimit; i++) {
            byte result = this.function_DO_1(this.severalMessageOffset);
            
            if (this.showTextFromMessage) {
                showResultText = true;
            }
            
            if (result == 0x10) {
                
                // dataCoding = headerSecondByte & 0x06 = 0
                if (this.dataCoding != CODING_2_UNICODE && this.dataCoding != CODING_4_UNICODE) {// latin
                    this.shortBuffer[5] = (short) (this.severalMessageOffset + this.messageBlockOffset);
                    
                } else {// cyrillic
                    this.shortBuffer[5] = 552;
                    // messageBlockOffset = 60
                    this.workWithBuffers(EXTENDED_BUFFER, (short) (this.severalMessageOffset + this.messageBlockOffset), EXTENDED_BUFFER, this.shortBuffer[5]);
                }
                
                if (source == SMS_PP && showResultText && sfield_token255_descoff268_staticref56 != this.firstByteMarkerClass && !this.showTextFromMessage) {
                    showResultText = false;
                    inProgressOnlyForICB = true;
                }
                
                if (this.playSoundFromMessage && showResultText && source != MENU_SELECTION) {
                    this.playTone();
                }
                
                this.processShowMessageAndParseResult(this.dataCoding != CODING_0_LATIN, this.shortBuffer[5], showResultText, source);
                // } else {
                // result = 0x12; // 0x12 = 18
                // this.parsedMsgBufferOffset = -6;
            }
            
            if (this.parsedMsgBufferOffset >= 0) {
                result = this.doCommandFromBuffer(PARSED_MESSAGE, this.parsedMsgBufferOffset);
            }
            
            if (this.moreCycles) {
                cycleLimit++;
                this.severalMessageOffset += this.messageLength;
            } else {
                this.severalMessageOffset += this.messageLength;
                
                if ((this.severalMessageOffset % 79) != 0) {
                    this.severalMessageOffset += (short) (79 - (this.severalMessageOffset % 79));
                }
            }
            
            if (this.parsedMsgBufferOffset == DISPLAY_TEXT_OFFSET_ERROR) {
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
        
        this.messageBlockOffset = (short) (tmpHeaderBuffer[0] & 0xFF);
        this.messageBlockOffset++;
        // messageBlockOffset = 0x3B + 1 = 60
        
        // true - if low byte is нечетный (1,3,5,7,9)
        this.moreCycles = (tmpHeaderBuffer[1] & 1) != 0;
        
        // -64 = 1100:0000
        // в tmpHeaderBuffer[1] старший бит должен быть 1,2 или 3 (0x32)
        // если биты 6 и 7 установлены, то выходим
        if ((tmpHeaderBuffer[1] & -64) != 0) { // 0xC0 = -64
            return -1;
        } else {
            byte messagePartsCount = 0;
            byte bufferSecondValue = 0;
            short lengthOfDataMessage = 0;
            
            switch ((tmpHeaderBuffer[1] & 0x30)) {
                case 0x10:
                case 0x30:
                    messagePartsCount = EXTENDED_BUFFER[bufferOffset + this.messageBlockOffset];
                    // messagePartsCount = EX[0 + 60] = 0x01
                    
                    this.messageLength = (short) (bufferOffset + this.messageBlockOffset + 1);
                    // messageLength = 0 + 60 + 1 = 61
                    
                    for (byte i = 0; i < messagePartsCount; i++) {
                        bufferSecondValue = EXTENDED_BUFFER[this.messageLength];
                        // bufferSecondValue = EX[61] = 0x02
                        
                        lengthOfDataMessage = (short) (EXTENDED_BUFFER[this.messageLength + bufferSecondValue] & 0xFF);
                        // lengthOfDataMessage = EX[61 + 2] = 0x11 & 0xFF = 17
                        
                        this.messageLength++;
                        // messageLength = 61 + 1 = 62
                        
                        this.messageLength = (short) (this.messageLength + bufferSecondValue + lengthOfDataMessage);
                        // messageLength = 62 + 2 + 17 = 81
                    }
                    
                    this.messageLength -= bufferOffset;
                    // messageLength = 81 - 0 = 81
                    
                    if (this.function_DO_1_1(bufferOffset, (tmpHeaderBuffer[1] & 0x30) == 0x30, tmpHeaderBuffer[1], tmpHeaderBuffer[2])) {
                        return 0x10; // 0x10, 0001:0000
                    }
                    break;
                case 0x20:
                    if (!this.isAddressInSmsTpduExist) {
                        return -2; // 0xFE, 1111:1110
                    }
                    
                    this.messageLength = this.messageBlockOffset;
                    byte var2 = this.processTechnicalMessage(bufferOffset, tmpHeaderBuffer[1], tmpHeaderBuffer[2]);
                    if (var2 == 0) {
                        return 0x20; // 0x20, 0010:0000
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
        // 0, true, 0x32, 0xE4
        
        this.typeMessageC = false;
        
        // 0x06 - check bit 1 and 2
        this.dataCoding = (byte) (headerSecondByte & 0x06);
        // 0x32 & 0x06 = 0x02
        
        byte serviceItemCount = (byte) (headerThirdByte & 0x0F);
        // 0xE4 & 0x0F = 0x04
        
        short parsedOffset = 0;
        short offset = (short) (bufferOffset + 1 + 2);
        // offset = 3
        
        PARSED_MESSAGE[31] = 0;
        
        if (var2) {
            if (this.method_token255_descoff1049(EXTENDED_BUFFER[offset++]) == 0) {
                return false;
            }
            // offset = 4
        }
        
        if (this.dataCoding == CODING_2_UNICODE) {
            // 0x7F80 = 32640
            // unicodePattern = EX[4] = 0x0B << 7 = 0x0580 & 0x7F80 = 0x0580
            this.unicodePattern = (short) ((EXTENDED_BUFFER[offset++] << 7) & 0x7F80);
            // offset = 5
            
        } else if (this.dataCoding == CODING_4_UNICODE) {
            this.unicodePattern = Util.getShort(EXTENDED_BUFFER, offset);
            offset = (short) (offset + 2);
        }
        
        if ((headerThirdByte & 0x80) != 0) {// 0x80 = -128 (0xE4 & 0x80 = 80)
            this.playSoundFromMessage = true;
        } else {
            this.playSoundFromMessage = false;
        }
        
        // 0x40, 0x50, 0x60, 0x70, 0xC0, 0xD0, 0xE0, 0xF0
        // для смс кажется должно быть ==0 чтобы текст показался
        if ((headerThirdByte & 0x40) != 0) {// 0x40 = 64 (0xE4 & 0x40 = 40)
            this.showTextFromMessage = true;
        } else {
            this.showTextFromMessage = false;
        }
        
        // check SMS access
        if ((headerThirdByte & 0x20) != 0) {// 0x20 = 32 (0xE4 & 0x20 = 20)
            if ((EXTENDED_BUFFER[offset] & techSMSaccessSmsAvailability) == 0 && EXTENDED_BUFFER[offset] != 0) {
                return false;
            }
            offset++;
            // offset = 6
        }
        
        // check ICB access
        if ((headerThirdByte & 0x10) != 0) {// 0x10 = 16 (0xE4 & 0x10 = 0)
            if ((EXTENDED_BUFFER[offset] & techSMSaccessIcbAvailability) == 0 && EXTENDED_BUFFER[offset] != 0) {
                return false;
            }
            offset++;
        }
        
        byte shortNumberLen = 0;
        
        if (serviceItemCount > 0) { // 4
            // offset 6 = 0x03
            this.shortNumber1LenGlobal = EXTENDED_BUFFER[offset++];
            // shortNumber1LenGlobal = 3
            // offset 7
            if (this.shortNumber1LenGlobal > 0) {
                shortNumberLen = this.shortNumber1LenGlobal;
                // shortNumberLen = 3
                if (this.shortNumber1LenGlobal > 11) {
                    this.shortNumber1LenGlobal = 11;
                }
                
                Util.arrayCopy(EXTENDED_BUFFER, offset, PARSED_MESSAGE, (short) 0, this.shortNumber1LenGlobal);
                offset = (short) (offset + shortNumberLen);
                // offset = 7 + 3 = 10
            }
            
            // offset 10 = 0x06
            this.shortNumber2LenGlobal = EXTENDED_BUFFER[offset++];
            // offset 11
            if (this.shortNumber2LenGlobal > 0) {
                shortNumberLen = this.shortNumber2LenGlobal;
                if (this.shortNumber2LenGlobal > 20) {
                    this.shortNumber2LenGlobal = 20;
                }
                
                Util.arrayCopy(EXTENDED_BUFFER, offset, PARSED_MESSAGE, (short) 11, this.shortNumber2LenGlobal);
                offset = (short) (offset + shortNumberLen);
                // offset = 11 + 6 = 17
            }
        }
        
        SIMView sv = SIMSystem.getTheSIMView();
        sv.select(SIMView.FID_MF);
        sv.select(SIMView.FID_DF_GSM);
        sv.select(DF_CELLTICK);
        sv.select(EF_APP_MESSAGES);
        
        if (techSMSvalue2 != 2 || flowState != READY) {
            
            short offsetBaseItem = this.findBaseItemsOffset(TAG_BASE_ITEM_NEXT);
            if (offsetBaseItem != -1) {
                // TAG_BASE_ITEM_NEXT = len 20
                parsedOffset = (short) (parsedOffset + this.readSimMessageValueToBuffer(offsetBaseItem, PARSED_MESSAGE, (short) (32 + parsedOffset), true));
                short savedParsedOffset = parsedOffset; // = 21
                
                parsedOffset++;
                PARSED_MESSAGE[32 + parsedOffset] = (byte) 0x80; // 0x80 = -128;
                parsedOffset++;
                PARSED_MESSAGE[32 + savedParsedOffset] = (byte) (parsedOffset - savedParsedOffset - 1); // (23 - 21 - 1) = 1
                PARSED_MESSAGE[31]++;
            }
        }
        
        short limit = 5;
        short length = 0;
        short var19 = 0;
        short dynamicMsgLen = 0;
        
        for (byte counter = 0; counter < serviceItemCount && counter < limit; counter++) {
            short nextByte = EXTENDED_BUFFER[offset];
            // 1: offset 17, NB = 0x15
            // 2: offset 24, NB = 0xA3
            // 3: offset 49, NB = 0x30
            // 4: offset 52, NB = 0x80
            
            short dataItemLength = 0;
            boolean continueProcess = true;
            boolean useBaseItemMessage = true;
            boolean var15 = false;
            short serviceType = (short) (nextByte & 0x70);
            // 1: ST = 0x15 & 0x70 = 0x10
            // 2: ST = 0xA3 & 0x70 = 0x20
            // 3: ST = 0x30 & 0x70 = 0x30
            // 4: ST = 0x80 & 0x70 = 0x00
            
            switch (serviceType) {
                case 0x00: // 0x80,0x00 & 0x70
                case 0x20: // 0xA0,0x20 & 0x70
                case 0x30: // 0xB0,0x30 & 0x70
                case 0x50: // 0xD0,0x50 & 0x70
                case 0x60: // 0xE0,0x60 & 0x70
                    if (serviceType == 0 && sfield_token255_descoff163_staticref38 != 2 && sfield_token255_descoff163_staticref38 != 3) {
                        useBaseItemMessage = false;
                    }
                    short firstPart = (short) (1 + (nextByte & 0x0F));
                    // 2: firstPart=4
                    // 3: firstPart=1
                    // 4: firstPart=1
                    short secondPart = (short) (1 + (EXTENDED_BUFFER[offset + firstPart] & 0x7F));
                    // 2: secondPart = 10
                    // 3: secondPart = 1
                    // 4: secondPart=1
                    dataItemLength = (short) (firstPart + secondPart);
                    // 2: dataItemLength = 14
                    // 3: dataItemLength = 2
                    // 4: dataItemLength = 2
                    break;
                case 0x10: // 0x90,0x10 & 0x70 // 0x10 = 16
                    dataItemLength = (short) (1 + (nextByte & 0x0F));
                    // 1: dataItemLength = 1 + 0x15 & 0x0F = 6
                    break;
                case 0x40: // 0xC0,0x40 & 0x70 // 0x40 = 64
                    this.typeMessageC = true;
                    dataItemLength = (short) (1 + (nextByte & 0x0F));
                    break;
                case 0x70: // 0xF0,0x70 & 0x70 //
                    serviceType = EXTENDED_BUFFER[offset + 2];
                    switch (serviceType) {
                        case 0x02:
                            // SEND USSD
                        case 0x03:
                            // SEND USSD
                            dataItemLength = 1;
                            
                            for (var19 = 0; dataItemLength < EXTENDED_BUFFER[offset + 1]; var19++) {
                                dataItemLength = (short) (dataItemLength + EXTENDED_BUFFER[offset + dataItemLength + 1 + 1] + 1);
                            }
                            
                            if (sfield_token255_descoff191_staticref42 && var19 >= 2) {
                                var15 = true; // continueProcess = true
                            }
                            
                            var19 = 0;
                            dataItemLength = EXTENDED_BUFFER[offset + 1];
                            
                            if (EXTENDED_BUFFER[offset + dataItemLength + 4] == 4) {
                                var19 = 1;
                            }
                            
                            dataItemLength = 1;
                    }
                    
                    if (var15) {
                        break;
                    }
                default:
                    limit++;
                    continueProcess = false;
                    offset = (short) (offset + 2 + EXTENDED_BUFFER[offset + 1]);
            } // end switch
            
            if (continueProcess) {
                if (useBaseItemMessage) {
                    PARSED_MESSAGE[31]++;
                    // 1: PA[31] = 1
                    // 2: PA[31] = 2
                    // 3: PA[31] = 3
                }
                
                boolean isHighValueLetter = ((nextByte & 0x80) != 0 ? true : false);
                // 1: isHighValueLetter = false (0x15 & 0x80)
                // 2: isHighValueLetter = true (0xA3 & 0x80)
                // 3: isHighValueLetter = false (0x30 & 0x80)
                // 4: isHighValueLetter = true (0x80 & 0x80)
                boolean baseItemAdded = true;
                
                if (!isHighValueLetter && (!var15 || serviceType != 2 && serviceType != 3 || var19 != 1)) {
                    if (useBaseItemMessage) {
                        short baseItemOffset = 0;
                        
                        // 1: var15=false
                        // 3: var15=false
                        if (var15 && (serviceType == 2 || serviceType == 3)) {
                            dynamicMsgLen = (short) (EXTENDED_BUFFER[offset + dataItemLength + 2] + 1);
                            baseItemOffset = this.findBaseItemsOffset(EXTENDED_BUFFER[offset + dataItemLength + 2 + dynamicMsgLen + 1]);
                        } else {
                            // 1: EX[17 + 6] = 0x01
                            // 3: EX[49 + 2] = 0x02
                            baseItemOffset = this.findBaseItemsOffset(EXTENDED_BUFFER[offset + dataItemLength]);
                            // 1: baseItemOffset (start index in file of item LLПозвонить)
                            // 3: baseItemOffset (start index in file of item LLПодробнее)
                        }
                        
                        if (baseItemOffset != -1) {
                            // 1: parsedOffset = 0
                            // 3: parsedOffset = 69
                            parsedOffset = (short) (parsedOffset + this.readSimMessageValueToBuffer(baseItemOffset, PARSED_MESSAGE, (short) (32 + parsedOffset), true));
                            // 1: parsedOffset = 00+read(startIndex, bufferDst, 32+00, true) = 00+21 = 21
                            // 3: parsedOffset = 69+read(startIndex, bufferDst, 32+69, true) = 69+21 = 90
                        } else {
                            baseItemAdded = false;
                            PARSED_MESSAGE[31]--;
                        }
                    }
                } else {
                    if (var15 && (serviceType == 2 || serviceType == 3)) {
                        dataItemLength = (short) (dataItemLength + 2);
                        
                        dynamicMsgLen = (short) (EXTENDED_BUFFER[offset + dataItemLength] + 1);
                        dataItemLength = (short) (dataItemLength + dynamicMsgLen);
                        
                        if (var19 == 1) {
                            
                            dynamicMsgLen = (short) (EXTENDED_BUFFER[offset + dataItemLength] + 1);
                            dataItemLength = (short) (dataItemLength + dynamicMsgLen + 1 + 1 + 1);
                            
                            dynamicMsgLen = (short) (EXTENDED_BUFFER[offset + dataItemLength] + 1);
                            dataItemLength = (short) (dataItemLength + dynamicMsgLen);
                            
                            length = EXTENDED_BUFFER[offset + dataItemLength];
                        } else {
                            length = EXTENDED_BUFFER[offset + dataItemLength];
                        }
                    } else {
                        // 2: offset = 24, dataItemLength = 14
                        // 4: offset = 52, dataItemLength = 2
                        length = (short) EXTENDED_BUFFER[offset + dataItemLength];
                        // 2: length = EX[24+14] = 0x0A = 10
                        // 4: length = EX[52+2] = 0x05 = 05
                        
                        dynamicMsgLen = (short) (length + 1);
                        // 2: dynamicMsgLen = 11
                        // 4: dynamicMsgLen = 6
                    }
                    
                    if (length > 40) {
                        length = 40;
                    }
                    
                    if (this.dataCoding != CODING_2_UNICODE && this.dataCoding != CODING_4_UNICODE) {
                        if (useBaseItemMessage) {
                            if (this.dataCoding == CODING_6_UNICODE) {
                                PARSED_MESSAGE[32 + parsedOffset] = (byte) (length + 1);
                                PARSED_MESSAGE[32 + parsedOffset + 1] = (byte) 0x80;
                                parsedOffset = (short) (parsedOffset + 2);
                            } else {
                                PARSED_MESSAGE[32 + parsedOffset] = (byte) length;
                                parsedOffset++;
                            }
                            
                            Util.arrayCopy(EXTENDED_BUFFER, (short) (offset + dataItemLength + 1), PARSED_MESSAGE, (short) (32 + parsedOffset), length);
                            
                            if (!var15) {
                                dataItemLength = (short) (dataItemLength + dynamicMsgLen);
                            }
                            
                            parsedOffset = (short) (parsedOffset + length);
                        }
                    } else {
                        if (useBaseItemMessage) {
                            // 2: offset = 24, dataItemLength = 14, parsedOffset = 30
                            length = this.allocateStringToBuffer(EXTENDED_BUFFER, (short) (offset + dataItemLength), PARSED_MESSAGE, (short) (32 + parsedOffset + 2));
                            // 2: length=20
                            
                            PARSED_MESSAGE[32 + parsedOffset] = (byte) ((length > 40 ? 40 : length) + 1);
                            // 2: PA[32+30] = 21
                            PARSED_MESSAGE[32 + parsedOffset + 1] = (byte) 0x80;
                            // 2: PA[32+30+1] = 80
                            
                            parsedOffset = (short) (parsedOffset + PARSED_MESSAGE[32 + parsedOffset] + 1);
                            // 2: parsedOffset = 30 + PA[32+30] + 1 = 30+21+1 = 52
                        }
                        
                        if (!var15) {
                            dataItemLength = (short) (dataItemLength + dynamicMsgLen);
                            // 2: dataItemLength = 14 + 11 = 25
                            // 4: dataItemLength = 2 + 6 = 8
                        }
                    }
                } // end if else
                
                if (!useBaseItemMessage) {
                    baseItemAdded = false;
                }
                
                short savedParsedOffset = 0;
                
                if (baseItemAdded) {
                    savedParsedOffset = parsedOffset;
                    // 1: savedParsedOffset = 21
                    // 2: savedParsedOffset = 52
                    // 3: savedParsedOffset = 90
                    parsedOffset++;
                    // 1: parsedOffset = 22
                    // 2: parsedOffset = 53
                    // 3: parsedOffset = 91
                    PARSED_MESSAGE[32 + parsedOffset] = (byte) serviceType;
                    // 1: PA[32+22] = ST = 0x10
                    // 2: PA[32+53] = ST = 0x20
                    // 3: PA[32+91] = ST = 0x30
                    parsedOffset++;
                    // 1: parsedOffset = 23
                    // 2: parsedOffset = 54
                    // 3: parsedOffset = 92
                }
                
                length = (short) (nextByte & 0x0F);
                // 1: length = 0x15 & 0x0F = 5
                // 2: length = 0xA3 & 0x0F = 3
                // 3: length = 0x30 & 0x0F = 0
                // 4: length = 0x80 & 0x0F = 0
                
                shortNumberLen = (byte) length;
                // 1: shortNumberLen = 5
                // 2: shortNumberLen = 3
                // 3: shortNumberLen = 0
                // 4: shortNumberLen = 0
                
                if (length > 11) {
                    length = 11;
                }
                
                short digitsCountInNumber;
                
                /*
                 * START
                 * COPY SHORT NUMBER FROM MESSAGE TO PARSED
                 */
                if (serviceType == 0 || serviceType == 0x10 || serviceType == 0x20 || serviceType == 0x30 || serviceType == 0x50 || serviceType == 0x60) {
                    if (baseItemAdded) {
                        if (length > 0) {
                            // 1:parsedOffset=23
                            // 2:parsedOffset=54
                            PARSED_MESSAGE[32 + parsedOffset] = (byte) (length + 1);
                            // 1: PA[32+23] = 5 + 1 = 6
                            // 2: PA[32+54] = 3 + 1 = 4
                            
                            if (serviceType != 0x10) {
                                digitsCountInNumber = (short) ((length - 1) * 2);
                                // 2:digitsCountInNumber = 4
                                
                                // если последняя цифра с F, т.е. не 2 а одна, то уменьшаем кол-во цифр на одну
                                if ((EXTENDED_BUFFER[offset + 1 + length - 1] & 0xF0) == 0xF0) {
                                    digitsCountInNumber--;
                                    // 2:digitsCountInNumber = 3
                                }
                            } else {
                                digitsCountInNumber = length;
                                // 1:digitsCountInNumber = 5
                            }
                            
                            parsedOffset++;
                            // 1:parsedOffset=24
                            // 2:parsedOffset=55
                            PARSED_MESSAGE[32 + parsedOffset] = (byte) digitsCountInNumber;
                            // 1: PA[32+24] = 5
                            // 2: PA[32+55] = 3
                            parsedOffset++;
                            // 1:parsedOffset=25
                            // 2:parsedOffset=56
                            
                            Util.arrayCopy(EXTENDED_BUFFER, (short) (offset + 1), PARSED_MESSAGE, (short) (32 + parsedOffset), length);
                            // 1: EX[17 + 1] copy PA[32 + 25] len=5
                            // 2: EX[24 + 1] copy PA[32 + 56] len=3
                            
                            parsedOffset = (short) (parsedOffset + length);
                            // 1: parsedOffset = 25 + 5 = 30
                            // 2: parsedOffset = 56 + 3 = 59
                        } else {
                            PARSED_MESSAGE[32 + parsedOffset] = (byte) (this.shortNumber1LenGlobal + 1);
                            // 3: PA[32+92]=4
                            
                            if (serviceType != 0x10) {
                                digitsCountInNumber = (short) ((this.shortNumber1LenGlobal - 1) * 2);
                                // 3: digitsCountInNumber = 4
                                
                                // если последняя цифра с F, т.е. не 2 а одна, то уменьшаем кол-во цифр на одну
                                if ((PARSED_MESSAGE[this.shortNumber1LenGlobal - 1] & 0xF0) == 0xF0) {
                                    digitsCountInNumber--;
                                }
                            } else {
                                digitsCountInNumber = this.shortNumber1LenGlobal;
                            }
                            
                            parsedOffset++;
                            // 3: parsedOffset=93
                            
                            PARSED_MESSAGE[32 + parsedOffset] = (byte) digitsCountInNumber;
                            // 3: PA[32+93]=4
                            
                            parsedOffset++;
                            // 3: parsedOffset=94
                            
                            Util.arrayCopy(PARSED_MESSAGE, (short) 0, PARSED_MESSAGE, (short) (32 + parsedOffset), this.shortNumber1LenGlobal);
                            // 3: PA[0] copy PA[32+94] len 3
                            
                            parsedOffset = (short) (parsedOffset + this.shortNumber1LenGlobal);
                            // 3: parsedOffset = 94+3 = 97
                        }
                    } // end if
                    
                    offset = (short) (offset + 1 + shortNumberLen);
                    // 1: offset = 17+1+5 = 23
                    // 2: offset = 24+1+3 = 28
                    // 3: offset = 49+1+0 = 50
                    // 4: offset = 52+1+0 = 53
                } // end if
                /*
                 * COPY SHORT NUMBER FROM MESSAGE TO PARSED
                 * FINISH
                 */
                
                /*
                 * START
                 * COPY CODE-PHRASE FOR SMS SEND FROM MESSAGE TO PARSED
                 */
                if (serviceType == 0x00 || serviceType == 0x20 || serviceType == 0x30 || serviceType == 0x50 || serviceType == 0x60) {
                    // 2: offset = 28
                    // 3: offset = 50
                    // 4: offset = 53
                    short lenByte = EXTENDED_BUFFER[offset];
                    // 2: lenByte = 0x09
                    // 3: lenByte = 0x80
                    // 4: lenByte = 0x80
                    
                    length = (short) (lenByte & 0x7F);
                    // 2: len=9
                    // 3: len=0
                    // 4: len=0
                    
                    byte codePhraseLen = (byte) length;
                    // 2: codePhraseLen=9
                    // 3: codePhraseLen=0
                    // 4: codePhraseLen=0
                    
                    if (length > 20) {
                        length = 20;
                    }
                    
                    if (baseItemAdded) {
                        if (length > 0) {
                            Util.arrayCopy(EXTENDED_BUFFER, (short) (offset + 1), PARSED_MESSAGE, (short) (32 + parsedOffset + 1), length);
                            // 2: EX[28+1] copy PA[32+59+1] len 9
                        } else {
                            length = this.shortNumber2LenGlobal;
                            // 3: length = 6
                            Util.arrayCopy(PARSED_MESSAGE, (short) 11, PARSED_MESSAGE, (short) (32 + parsedOffset + 1), length);
                            // 3: PA[11] copy PA[32+97+1] len 6
                        }
                        
                        // срабатывает если high value > 8, 0x80,0x90,0xA0,0xB0,0xC0,0xD0,0xE0,0xF0
                        // проставляем в конце итема нумерацию в зависимости от значения цикла
                        if ((lenByte & 0x80) != 0) {
                            PARSED_MESSAGE[32 + parsedOffset + length + 1] = 0x20; // space
                            // 3: PA[32+97+6+1]=0x20
                            PARSED_MESSAGE[32 + parsedOffset + length + 2] = (byte) (counter + 1 + 0x30); // 0x31 = 1, 0x32 = 2....
                            // 3: PA[32+97+6+2]=2+1+0=3
                            length = (short) (length + 2);
                            // 3: length=8
                        }
                        
                        if (serviceType == 0) {
                            PARSED_MESSAGE[32 + parsedOffset + length + 1] = 0x20;
                            PARSED_MESSAGE[32 + parsedOffset + length + 2] = (byte) (sfield_token255_descoff163_staticref38 + 48);
                            length = (short) (length + 2);
                        }
                        
                        PARSED_MESSAGE[32 + parsedOffset] = (byte) length;
                        // 2: PA[32+59] = 9
                        // 3: PA[32+97] = 8
                        
                        parsedOffset = (short) (parsedOffset + length + 1);
                        // 2: parsedOffset = 59+9+1 = 69
                        // 3: parsedOffset = 97+8+1 = 106
                    } // end if
                    
                    offset = (short) (offset + codePhraseLen + 1);
                    // 2: offset=28+9+1=38
                    // 3: offset=50+0+1=51
                    // 4: offset=53+0+1=54
                } // end if
                /*
                 * COPY CODE-PHRASE FOR SMS SEND FROM MESSAGE TO PARSED
                 * FINISH
                 */
                
                if (serviceType == 2 || serviceType == 3) {
                    length = EXTENDED_BUFFER[offset + 1 + 2];
                    shortNumberLen = (byte) length;
                    if (length > 20) {
                        length = 20;
                    }
                    
                    if (baseItemAdded) {
                        if (length > 0) {
                            Util.arrayCopy(EXTENDED_BUFFER, (short) (offset + 1 + 3), PARSED_MESSAGE, (short) (32 + parsedOffset + 1), length);
                        } else {
                            length = this.shortNumber2LenGlobal;
                            Util.arrayCopy(PARSED_MESSAGE, (short) 11, PARSED_MESSAGE, (short) (32 + parsedOffset + 1), length);
                        }
                        
                        PARSED_MESSAGE[32 + parsedOffset] = (byte) length;
                        parsedOffset = (short) (parsedOffset + length + 1);
                    }
                } // end if
                
                // 1: var15 = false
                // 2: var15 = false
                // 3: var15 = false
                if (var15) {
                    offset = (short) (offset + EXTENDED_BUFFER[offset + 1] + 1 + 1);
                    if (var19 == 1) {
                        offset = (short) (offset + EXTENDED_BUFFER[offset + 1] + 1 + 1);
                        limit++;
                        counter++;
                    }
                } else {
                    if (serviceType == 0x40) {
                        offset++;
                    }
                    
                    // 1: isHighValueLetter = false
                    // 2: isHighValueLetter = true
                    // 3: isHighValueLetter = false
                    // 4: isHighValueLetter = true
                    if (isHighValueLetter) {
                        offset = (short) (offset + dynamicMsgLen);
                        // 2: offset = 54+6 = 60
                    } else {
                        offset++;
                        // 1: offset = 24
                        // 3: offset = 52
                    }
                } // end if
                
                if (baseItemAdded) {
                    // 1: parsedOffset = 30, savedParsedOffset = 21
                    // 2: parsedOffset = 69, savedParsedOffset = 52
                    // 3: parsedOffset = 106, savedParsedOffset = 90
                    PARSED_MESSAGE[32 + savedParsedOffset] = (byte) (parsedOffset - savedParsedOffset - 1);
                    // 1: PA[32+21] = 30-21-1 = 8
                    // 2: PA[32+52] = 69-52-1 = 16
                    // 3: PA[32+90] = 106-90-1 = 15
                }
            } // end if
        } // end for
        
        /* FINISH */
        
        if (flowState != READY || this.showTextFromMessage) {
            short itemOffset = this.findBaseItemsOffset(TAG_BASE_ITEM_MAIN);
            if (itemOffset != -1) {
                parsedOffset = (short) (parsedOffset + this.readSimMessageValueToBuffer(itemOffset, PARSED_MESSAGE, (short) (32 + parsedOffset), true));
                short savedParsedOffset = parsedOffset;
                
                parsedOffset++;
                PARSED_MESSAGE[32 + parsedOffset] = (byte) 0x90; // -112
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
