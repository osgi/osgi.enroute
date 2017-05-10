# OSGI ENROUTE IOT CIRCUIT APPLICATION

${Bundle-Description}

## Preliminary

The IoT Circuit application models a _circuit board_. ICs on this board are provided by OSGi DS _components_. A component must implement the IC service interface. The ICAdapter provides an easy way to create standard ICs. Since the ICs are components, they can easily be driven by configuration. For an example see the Counter in the `osgi.enroute.iot.circuit.toolkit` package. 

Hardware drivers create ICs that are only input or only output. For example, the `osgi.enroute.iot.pi.provider` provides ICs (GPIOs) for the general purpose inputs and outputs. On the Pi bundle, the inputs are configured through Configuration Admin. For each board type, you can set the configuration for each pin.

You find that you'll need the following bundles:

* osgi.enroute.iot.circuit.provider – Provides the Circuit Admin & Circuit Board servers
* osgi.enroute.iot.circuit.command – Some commands for manipulating the board
* osgi.enroute.iot.pi.provider – Provides the hardware driver to provide the General Purpose I/O on the Raspberry Pi
* osgi.enroute.iot.pi.command – Some commands to play with the Raspberry Pi
* osgi.enroute.iot.circuit.application – The UI

## How to Run?
To described case is running this on a Mac with a Raspberry Pi on the Ethernet port. Through the System Preferences (Sharing) you can share your Wifi access on the Ethernet port. This enables DHCP. In my case, the Raspberry Pi got IP number 192.168.2.4. Your mileage may vary.    

Login to the Pi. 

Ok, now get Java 8 installed. You can find LOTS of stuff on the Internet, I find the following the easiest (you might want to find the latest version URL from [JDK 8 Arm Downloads][3].):

	$ curl -v -j -k -L -o jre.tar.gz -H \
		Cookie:oraclelicense=accept-securebackup-cookie \
		http://download.oracle.com/otn-pub/java/jdk/8u33-b05/jdk-8u33-linux-arm-vfp-hflt.tar.gz
	$ tar xzvf jre.tar.gz
	$ mv jdk1.8.0_33 jre
	$ export JAVA_HOME=/home/pi/jre
	$ export PATH=$JAVA_HOME/bin:$PATH
	
Then install [jpm][2].

	$ curl http://www.jpm4j.org/install/global | sudo sh

Then install the latest (staging (that's the @*)) bndremote program:

	$ sudo jpm install bndremote@*
	$ sudo bndremote -etn 192.168.2.4
	Listening for transport dt_socket at address: 1044
	# Will wait  for /192.168.2.4:29998 to finish
	
If jpm does not work for you, the you could also download [the remote launcher main program on the Raspberry Pi][1]. For example:

	$ curl -o remotemain.jar https://bndtools.ci.cloudbees.com/job/bnd.master/lastSuccessfulBuild/
		artifact/dist/bundles/biz.aQute.remote.main/biz.aQute.remote.main-latest.jar

We're now ready to start debugging on the Pi. Look at `osgi.enroute.iot.circuit.bndrun` and when necessary adapt the `-runremote` instruction to match your environment.

Select `osgi.enroute.iot.circuit.bndrun` and do `Debug As/Bnd Native`.  
You will first have to configure the Raspberry Pi. Go to 

	http://192.168.2.4:8080/system/console/configMgr
	
Select the `Pi 2 Model B` (it should also work for the Pi 1 Model B+, but this might require work) entry and fill in the data. The form shows you the actual pin nr and the logical name given by the WiringPi project (which is used to power P4J, which we use in the current implementation). 

I generally work work with the first 3 pins 

	pin 3 – GPIO08 out, high
	pin 5 – GPIO09 in, high
	pin 7 – GPIO07 out, high

If you're lucky, it all works and you can go to the GUI

	http://192.168.2.4:8080/osgi.enroute.iot.circuit

You should now 2 see 2 outputs and one input displayed. You can connect the outputs to the input by draggin output and drop it on an input. 

Alternatively, you can also use the commands to manipulate the circuit board (this should automatically update the GUI though). These commands are implemented in `osgi.enroute.iot.circuit.command.CircuitCommand`. Type `circuit:circuit` to see the commands available. 

	wires                           – Show the existing wires
	ics                             – Show the current ICs
	connect <from> <pin> <to> <pin> – Connect two ics
	gpo <id>                        – Create a test output to the Console
	clock <id>                      – Create a test clock
	disconnect id                   – Disconnect a write

There are also a number of Pi commands, type `pi:pi`. These are not very coherent yet ...

	pi:* commands. These directly manipulate the GpioController.
	create <name> <pin>           – create a digital pin
	test <name>                   – set a pin low/high 20 times
	pins                          – show the pins
	blink <name> <time>           – use the Pi4J blink function
	high <name>                   – set a pin high
	low <name>                    – set a pin low
	info                          – show all the info of the board
	reset                         – reset the controller

You can find the commands in `osgi.enroute.iot.pi.command.PiCommand`.

[1]: http://jpm4j.org/#!/p/osgi/biz.aQute.remote.launcher
[2]: http://jpm4j.org/#!/md/install
[3]: http://www.oracle.com/technetwork/java/javase/downloads/jdk8-arm-downloads-2187472.html
