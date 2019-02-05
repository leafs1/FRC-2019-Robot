/*----------------------------------------------------------------------------*/
/* Copyright (c) 2017-2018 FIRST. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/
//network table -> Check if the XY coords are cenetered. If not, turn until they are centered.
package frc.robot;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.Spark;
import edu.wpi.first.wpilibj.SpeedController;
import edu.wpi.first.wpilibj.SpeedControllerGroup;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
// Custom Class

/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to each mode, as described in the TimedRobot
 * documentation. If you change the name of this class or the package after
 * creating this project, you must also update the build.gradle file in the
 * project.
 */
public class Robot extends TimedRobot {
  private static final String kDefaultAuto = "Default";
  private static final String kCustomAuto = "My Auto";
  private String m_autoSelected;
  private final SendableChooser<String> m_chooser = new SendableChooser<>();


  // SpeedController Object creations - Define all names of motors here
	SpeedController leftFront, leftBack, rightFront, rightBack, gripper;
	// Speed controller group used for new differential drive class
	SpeedControllerGroup leftChassis, rightChassis;
	// DifferentialDrive replaces the RobotDrive Class from previous years
  DifferentialDrive chassis;
  
  // Both encoder sides
  Encoder leftEncoder, rightEncoder;
  // Number of counts per inch
  final static double ENCODER_COUNTS_PER_INCH = 13.49;

  // Creates Joystick buttons
  Boolean aButton, bButton, xButton, yButton;
  // Creates the driver's joystick
  Joystick driver;


  // Creates the network tables object
  NetworkTableInstance inst;
  // A specific table in network tables
  NetworkTable table;
  // A specific entry for the table
  NetworkTableEntry xEntry;
  NetworkTableEntry yEntry;
  // Variable that stores half way value of the screen
  int middlePixel = 320;

  enum AutoMovement {
    STRAIGHT,
    TURN,
    VISION
  }
  Object[][] autoTemplate = {
    // Movement type, Distance, Speed
    {AutoMovement.STRAIGHT, 200, 1},
    // Movement type, Rotation, Speed
    {AutoMovement.TURN, 90, 0.5}
  };
  // Dictates the current auto that is selected
  Object[][] selectedAuto;
  // Indicates what step of auto the robot is on
  int moveStep;
  // Indicates when auto should stop
  boolean autoStop;
  // Auto Function Variables
  AutoMovement moveType;
  double special;
  double speed;

  /**
   * This function is run when the robot is first started up and should be used
   * for any initialization code.
   */
  @Override
  public void robotInit() {
    m_chooser.setDefaultOption("Default Auto", kDefaultAuto);
    m_chooser.addOption("My Auto", kCustomAuto);
    SmartDashboard.putData("Auto choices", m_chooser);


    // Defines all the ports of each of the motors
		leftFront = new Spark(0);
		leftBack = new Spark(1);
		rightFront = new Spark(2);
		rightBack = new Spark(3);
		gripper = new Spark(4);
		// Defines the left and right SpeedControllerGroups for our DifferentialDrive class
		leftChassis = new SpeedControllerGroup(leftFront, leftBack);
		rightChassis = new SpeedControllerGroup(rightFront, rightBack);
		// Inverts the right side of the drive train to account for the motors being physically flipped
		rightChassis.setInverted(true);
		// Defines our DifferentalDrive object with both sides of our drivetrain
    chassis = new DifferentialDrive(leftChassis, rightChassis);

    // Setting encoder ports
    leftEncoder = new Encoder(0,1);
		rightEncoder = new Encoder(2,3);
    
    // Sets the joystick port
    driver = new Joystick(0);
    // Controls
    aButton = driver.getRawButton(1);
    bButton = driver.getRawButton(2);
    xButton = driver.getRawButton(3);
    yButton = driver.getRawButton(4);


    // Get default instance of automatically created Network Tables
    inst = NetworkTableInstance.getDefault();
    // Get the table within the instance that contains the data
    table = inst.getTable("visionTable");
    // Get X and Y entries
    xEntry = table.getEntry("xEntry");
    yEntry = table.getEntry("yEntry");
  }

  /**
   * This function is called every robot packet, no matter the mode. Use this for
   * items like diagnostics that you want ran during disabled, autonomous,
   * teleoperated and test.
   *
   * <p>
   * This runs after the mode specific periodic functions, but before LiveWindow
   * and SmartDashboard integrated updating.
   */
  @Override
  public void robotPeriodic() {
    // print compressor status to the console
    //System.out.println(enabled + "/n" + pressureSwitch + "/n" + current);

    //System.out.println("Im In");
  }

  /**
   * This autonomous (along with the chooser code above) shows how to select
   * between different autonomous modes using the dashboard. The sendable chooser
   * code works with the Java SmartDashboard. If you prefer the LabVIEW Dashboard,
   * remove all of the chooser code and uncomment the getString line to get the
   * auto name from the text box below the Gyro
   *
   * <p>
   * You can add additional auto modes by adding additional comparisons to the
   * switch structure below with additional strings. If using the SendableChooser
   * make sure to add them to the chooser code above as well.
   */
  @Override
  public void autonomousInit() {
    m_autoSelected = m_chooser.getSelected();
    // m_autoSelected = SmartDashboard.getString("Auto Selector", kDefaultAuto);
    System.out.println("Auto selected: " + m_autoSelected);

    // The step that auto is on
    moveStep = 0;

    // Which auto are we using?
    selectedAuto = autoTemplate;
    // Adds info for the first auto step
    assignAutoVariables(moveStep);

    // Has the auto finished?
    autoStop = false;
  }

  /**
   * This function is called periodically during autonomous.
   */
  @Override
  public void autonomousPeriodic() {
    if (!autoStop){
      if (autoRun()){
        moveStep++;
        if (moveStep < selectedAuto.length) {
          assignAutoVariables(moveStep);
        } else {autoStop = true;}
      }
    }
  }

  /**
   * This function is called periodically during operator control.
   */
  @Override
  public void teleopPeriodic() {

    if (yButton == true) {
      xEntry.setDouble(xEntry.getDouble(1)+1);
    }
    
    int threshold = 15;

    // drive according to vision input
    if (xEntry.getDouble(0.0) < middlePixel + threshold) {
      System.out.println("Turning Left " + xEntry.getDouble(middlePixel));
      // turn left
    } else if (xEntry.getDouble(0.0) > middlePixel - threshold) {
      System.out.println("Turning Right " + xEntry.getDouble(middlePixel));
      // turn right
    } else {
      System.out.println("Driving Straight " + xEntry.getDouble(middlePixel));
      // drive straight
    }
  }

  /**
   * This function is called periodically during test mode.
   */
  @Override
  public void testPeriodic() {
  }

  public void assignAutoVariables(int moveStepPar) {
    moveType = (AutoMovement) selectedAuto[moveStepPar][0];
    special = (double) selectedAuto[moveStepPar][1];
    special = (double) selectedAuto[moveStepPar][2];
  }

  public boolean autoRun() {
    if (moveType == AutoMovement.STRAIGHT) {
      if (getDistance() > special){
        return true;
      } else {
        chassis.arcadeDrive(speed, getHeading());
        return false;
      }
    } else if (moveType == AutoMovement.TURN){
      return false;
    }
    return false;
  }

  public double getDistance(){
		return ((double)(leftEncoder.get() + rightEncoder.get()) / (ENCODER_COUNTS_PER_INCH * 2));
  }
  
  public double getHeading() {
    // Add in heading code
    return 123.2;
  }
}
