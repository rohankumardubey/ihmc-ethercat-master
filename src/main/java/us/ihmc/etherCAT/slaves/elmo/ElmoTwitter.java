package us.ihmc.etherCAT.slaves.elmo;

import us.ihmc.etherCAT.master.ReadSDO;
import us.ihmc.etherCAT.master.RxPDO;
import us.ihmc.etherCAT.master.TxPDO;
import us.ihmc.etherCAT.slaves.DSP402Slave;

/**
 * Elmo twitter slave code.
 * 
 * Requires firmware version 1.1.12.b01 or above
 * 
 * @author jesper
 *
 */
public abstract class ElmoTwitter extends DSP402Slave
{

   static final int vendorID = 0x0000009a;
   static final int productCode = 0x00030924;

   
   public class RPDO_1605 extends RxPDO
   {
      public RPDO_1605()
      {
         super(0x1605);
      }
      
      Signed32 targetPosition = new Signed32();
      Signed32 targetVelocity = new Signed32();
      Signed16 targetTorque = new Signed16();
      Unsigned16 maxTorque = new Unsigned16();
      Unsigned16 controlWord = new Unsigned16();
      Signed8 modeOfOperation = new Signed8();
      Unsigned8 dummyByte = new Unsigned8();
   }
   

   public class RPDO_1606 extends RxPDO
   {
      public RPDO_1606()
      {
         super(0x1606);
      }

      Signed32 targetPosition = new Signed32();
      Unsigned32 digitalOutputs = new Unsigned32();
      Signed32 targetVelocity = new Signed32();
      Signed32 velocityOffset = new Signed32();
      Signed16 torqueOffset = new Signed16();
      Unsigned16 controlWord = new Unsigned16();
   }
   
   public class RPDO_160B extends RxPDO
   {
      public RPDO_160B()
      {
         super(0x160B);
      }

      Signed8 modeOfOperation = new Signed8();
      Unsigned8 dummy = new Unsigned8();
   }
   public class RPDO_161D extends RxPDO
   {
      public RPDO_161D()
      {
         super(0x161D);
      }
      
      Unsigned32 digitalOutputs = new Unsigned32();
   }
   
   public class TPDO_1a03 extends TxPDO
   {
      public TPDO_1a03()
      {
         super(0x1a03);
      }

      Signed32 positionActualValue = new Signed32();
      Unsigned32 digitalInputs = new Unsigned32();
      Signed32 velocityActualValue = new Signed32();
      Unsigned16 statusWord = new Unsigned16();

   }

   public class TPDO_1a12 extends TxPDO
   {
      public TPDO_1a12()
      {
         super(0x1a12);
      }
      Signed16 torqueDemand = new Signed16();

   }
   public class TPDO_1a13 extends TxPDO
   {
      public TPDO_1a13()
      {
         super(0x1a13);
      }

      Signed16 torqueActualValue = new Signed16();
   }
   
   public class TPDO_1a19 extends TxPDO
   {
      public TPDO_1a19()
      {
         super(0x1a19);
      }

      Signed32 positionFollowingError = new Signed32();
   }
   
   public class TPDO_1a1d extends TxPDO
   {
      public TPDO_1a1d()
      {
         super(0x1a1d);
      }
      
      Signed16 analogInput = new Signed16();

   }
   public class TPDO_1a1e extends TxPDO
   {
      public TPDO_1a1e()
      {
         super(0x1a1e);
      }

      Signed32 auxiliaryPositionActualValue = new Signed32();
   }
   
   
   public class TPDO_1a22 extends TxPDO
   {
      public TPDO_1a22()
      {
         super(0x1a22);
      }

      Unsigned32 elmoStatusRegister = new Unsigned32();
   }
   
   public class TPDO_1a18 extends TxPDO
   {
      public TPDO_1a18()
      {
         super(0x1a18);
      }
      
      Unsigned32 dcLinkVoltage = new Unsigned32();
   }


   
   //Status Register Event Locations
   private final long UNDER_VOLTAGE = 3;
   private final long OVER_VOLTAGE = 5;
   private final long STO_DISABLED = 7;
   private final long CURRENT_SHORT = 11;
   private final long OVER_TEMPERATURE = 13;
   private final long MOTOR_ENABLED = 16;
   private final long MOTOR_FAULT = 64;
   private final long CURRENT_LIMITED = 8192;
   
   private boolean errorCodeRead = false;
   private final ReadSDO elmoErrorCodeSDO;
   private int elmoErrorCode = 0;
   
   private boolean readElmoErrorCode = true;
   
   public ElmoTwitter(int alias, int position)
   {
      super(vendorID, productCode, alias, position);
      
      elmoErrorCodeSDO = new ReadSDO(this, 0x306A, 0x1, 4);
      
      // TODO: This can result in slaves going offline
      registerSDO(elmoErrorCodeSDO);
   }

   public abstract long getElmoStatusRegister();
   

   public boolean isFaulted()
   {
      return isUnderVoltage() || isOverVoltage() || isSTODisabled() || isCurrentShorted() || isOverTemperature() || isMotorFaulted();
   }

   @Override
   public void doStateControl()
   {
      super.doStateControl();
      
      
      if(elmoErrorCodeSDO.isValid())
      {
         elmoErrorCode = (int)elmoErrorCodeSDO.getUnsignedInt();
      }
      
      if(readElmoErrorCode && isMotorFaulted())
      {
         if (!errorCodeRead)
         {
            errorCodeRead = elmoErrorCodeSDO.requestNewData();
         }
      }
      else
      {
         errorCodeRead = false;
      }
      
      
   }
   
   public void disableElmoErrorCodeReading()
   {
      readElmoErrorCode = false;
   }

   private boolean getStatusValue(long maskValue)
   {
      //Bitwise AND to check bit-string for status events 
      return (getElmoStatusRegister() & 0xF) == maskValue;
   }
   
   //Status Register Events
   public boolean isUnderVoltage()
   {
      return getStatusValue(UNDER_VOLTAGE);
   }

   public boolean isOverVoltage()
   {
      return getStatusValue(OVER_VOLTAGE);
   }

   public boolean isSTODisabled()
   {
      return getStatusValue(STO_DISABLED);
   }

   public boolean isCurrentShorted()
   {
      return getStatusValue(CURRENT_SHORT);
   }

   public boolean isOverTemperature()
   {
      return getStatusValue(OVER_TEMPERATURE);
   }

   public boolean isMotorEnabled()
   {
      return getStatusValue(MOTOR_ENABLED);
   }

   public boolean isMotorFaulted()
   {
      return getStatusValue(MOTOR_FAULT);
   }

   public boolean isCurrentLimited()
   {
      return getStatusValue(CURRENT_LIMITED);
   }
   
   public int getLastElmoErrorCode()
   {
      return elmoErrorCode;
   }
   
   public String getLastElmoErrorCodeString()
   {
      return ElmoErrorCodes.errorCodeToString(getLastElmoErrorCode());
   }
   
   @Override 
   protected boolean hasShutdown()
   {
      if(super.hasShutdown())
      {
         int error = readSDOInt(0x306A, 0x1);
         int temperature = readSDOUnsignedShort(0x22A3, 0x1);
         System.out.println(toString() + " Last elmo error code: " + error + ": " + ElmoErrorCodes.errorCodeToString(error) + ". Drive temperature: " + temperature + "??C");
         return true;
      }
      return false;
   }
   
   @Override
   public final boolean supportsCA()
   {
      return false;
   }


}
