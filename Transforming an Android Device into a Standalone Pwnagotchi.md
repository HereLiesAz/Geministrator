

# **Transforming an Android Device into a Standalone Pwnagotchi**

## **Architectural Assessment of the "Pwnagotchi Android" Client**

The provided "Pwnagotchi Android" application serves as a well-architected foundation for the transformation into a standalone Pwnagotchi unit. Its current design as a remote user interface provides a robust scaffold upon which the on-device engine can be built, requiring a significant re-engineering of its backend logic rather than a complete rewrite of the application.

### **Deconstruction of the Existing Kotlin/Compose Codebase**

An analysis of the source code reveals a modern Android application developed with Kotlin and the Jetpack Compose UI toolkit.1 The project targets a recent Android SDK version (compileSdk \= 36\) and employs a contemporary architectural pattern resembling Model-View-ViewModel (MVVM). A PwnagotchiViewModel is responsible for managing the UI state, which is encapsulated within a PwnagotchiUiState sealed class. This state is observed reactively by the Compose UI through the use of collectAsState, a standard and efficient practice for building declarative UIs in Android.

The core networking and data processing logic is correctly isolated within a background PwnagotchiService. This service currently houses a WebSocketClient, built using the org.java-websocket library, designed to connect to a remote Pwnagotchi's WebSocket server. This service represents the primary component that will undergo a complete architectural overhaul to support on-device operations.1

### **Analysis of its Role as a Remote UI and Data Sink**

In its current form, the application functions exclusively as a passive display and remote control. The PwnagotchiService is designed to parse incoming JSON messages from a WebSocket connection, specifically listening for events with the types "ui\_update", "handshake", and "plugin\_list".1 This data contract defines the exact information that the new on-device engine must produce to be compatible with the existing UI. All user actions, such as toggling a plugin via the togglePlugin function, are marshaled into JSON command objects and transmitted over the WebSocket to the external Pwnagotchi device. This command-and-control mechanism will be fundamentally replaced by direct, local execution of shell commands.

### **Evaluating Foundational Elements for Repurposing**

Several key components of the existing application are not merely present but are foundational for its transformation into a standalone tool.

* **The libsu Dependency**: The most significant element is the inclusion of the com.github.topjohnwu.libsu:core library.1 This library is the cornerstone for achieving the project's goal, providing a robust API for executing commands with superuser (root) privileges. The current implementation only uses this for a rudimentary status check in MainActivity's onRequestRoot function, which verifies if root access is available but does not integrate it into any core functionality.1 In the new architecture, this library will be elevated from a simple utility to the central pillar of the application's orchestration layer, responsible for managing the entire lifecycle of the on-device Pwnagotchi engine.  
* **The Android Service**: The implementation of a foreground Service is a critical and correct architectural decision.1 Android's operating system is known for its aggressive process management to conserve battery, which would otherwise terminate the long-running monitoring tasks required by a Pwnagotchi. By running as a foreground service, the application signals to the OS that it is performing a user-initiated, long-running task that should not be killed. This service will be repurposed from a simple network client into a master process orchestrator, managing the lifecycle of the on-device bettercap and Python processes.  
* **UI State Management**: The PwnagotchiUiState sealed class is well-conceived and can be almost entirely reused.1 The defined states—Connecting, Connected, Disconnected, and Error—are abstract enough to represent the status of a local, on-device engine just as effectively as they represent a remote network connection. This allows for the complete preservation of the existing UI and its logic with minimal changes.

The existing application, therefore, is not just a starting point but a nearly complete front-end waiting for its local backend. The original Pwnagotchi project is powered by the bettercap tool, which itself exposes a WebSocket API for event streaming.2 The most direct and elegant path to transformation is to execute bettercap locally on the Android device and have the PwnagotchiService's WebSocketClient connect to the loopback address (ws://127.0.0.1) instead of a remote IP. This strategy leverages the entire existing UI and state management layer, dramatically simplifying the development effort. The presence of libsu in the project's dependencies confirms the foresight of this approach and its fundamental feasibility.1

## **The Android Wi-Fi Stack and the Monitor Mode Imperative**

Transforming the Android device requires operating its Wi-Fi hardware in a manner fundamentally unsupported by the standard Android software development kit. This necessitates bypassing the conventional Android framework through privileged access and relying on specialized, low-level tools that can directly interface with the wireless chipset.

### **Limitations of Standard Android APIs**

The standard Android networking APIs, primarily the ConnectivityManager class, are designed for high-level application use cases.4 These APIs allow an app to query the state of network connections (e.g., is the device connected to Wi-Fi or cellular data?) and monitor for changes in connectivity. However, they operate within a strict security sandbox that isolates applications from the underlying hardware. These APIs provide no functions for capturing raw 802.11 frames, viewing management or control frames, or placing the wireless interface into promiscuous or "monitor" mode. Consequently, these standard Android APIs are entirely unsuitable for the packet sniffing and injection tasks required by a Pwnagotchi.

### **The Necessity of Root Access and Chipset Compatibility**

To circumvent the limitations of the Android security model and gain direct control over the Wi-Fi hardware, root access is an absolute prerequisite.5 However, root access alone is insufficient. The single greatest determining factor for the success of this project is the device's hardware. The internal Wi-Fi chipset must be supported by a framework capable of enabling monitor mode through firmware patching. Research and community efforts overwhelmingly point to the **Nexmon framework** as the premier solution for the Broadcom and Cypress Wi-Fi chips commonly used in many Android devices.7 If the device's chipset is not supported by Nexmon or a similar framework, enabling monitor mode on the internal radio is impossible. The only alternative in such cases is to use an external USB Wi-Fi adapter with a compatible chipset, connected via a USB OTG cable.8

### **Deep Dive into the Nexmon Framework**

Nexmon achieves its functionality by replacing the device's stock Wi-Fi firmware with a custom, patched version. This new firmware exposes advanced capabilities, including monitor mode and frame injection, that are not present in the original firmware.7

* **Installation via Magisk**: The most reliable and modern method for installing Nexmon is through a Magisk module.3 Magisk is a "systemless" rooting solution, meaning it modifies the device's boot partition in memory without altering the read-only /system partition. This approach is safer, more resilient to system updates, and easier to uninstall than older rooting methods.3 The Nexmon Magisk module typically bundles the patched firmware, a command-line utility named nexutil for controlling the firmware, and any necessary SELinux policy modifications to allow the tools to function correctly.7 It is crucial to understand that the installation of these prerequisites—rooting with Magisk and installing the Nexmon module—is a manual setup process that the user must perform. The Pwnagotchi Android application will operate under the assumption that this environment is already in place.  
* **nexutil Command-Line Tool**: Once Nexmon is installed, the nexutil binary is the primary tool for interacting with the patched firmware. This utility replaces tools like airmon-ng, which are standard on desktop Linux distributions like Kali Linux but are not part of a typical Android system.9 The Pwnagotchi application will use its root shell to invoke nexutil to control the state of the wireless interface.

### **Procedural Guide to Wireless Interface Management**

The repurposed PwnagotchiService will leverage its persistent root shell, provided by the libsu library, to execute a precise sequence of commands to manage the wlan0 interface. This orchestration is critical to prevent conflicts with the standard Android Wi-Fi subsystem.

* **Enabling Monitor Mode**:  
  1. svc wifi disable: This command uses the Android service manager (svc) to gracefully stop the main Wi-Fi service. This is the preferred method as it prevents the Android framework from attempting to reconfigure the interface while it is being used for monitoring.5 It is a cleaner approach than manually killing the wpa\_supplicant process.9  
  2. ifconfig wlan0 up: This standard Linux command ensures the wireless interface is active at the kernel level before further configuration.12  
  3. nexutil \-m2: This is the core Nexmon command that instructs the patched firmware to enter full monitor mode with Radiotap headers, which provide valuable metadata for each captured packet.7  
* **Disabling Monitor Mode (Restoring Normalcy)**:  
  1. nexutil \-m0: This command instructs the firmware to return to the standard "managed" mode, where it can connect to access points.5  
  2. svc wifi enable: This command restarts the Android Wi-Fi service, allowing the system to regain control of the interface and automatically reconnect to known networks.5

The project's viability is therefore entirely dependent on a specific stack of prerequisites: a compatible device, a rooted operating system with Magisk, and a correctly installed Nexmon module. The Android application itself is the final layer that orchestrates this pre-existing, modified environment.

## **Designing the On-Device Pwnagotchi Engine**

To function as a standalone unit, the application must bundle and manage the core components of the Pwnagotchi software: the bettercap reconnaissance tool, which serves as the operational "hands," and the Python-based AI, which acts as the "brain." The Android Service will be the orchestrator that brings these components to life within the app's sandboxed environment.

### **Component Integration Strategy**

The on-device Pwnagotchi will be a composite system. Each component requires a specific integration method, and the Android application has distinct responsibilities for managing each one.

**Table: Component Integration Strategy**

| Component | Source / Technology | Integration Method | Android App Responsibility |
| :---- | :---- | :---- | :---- |
| **bettercap Engine** | Go binary (ARM64) | Bundled in app/src/main/assets | Extract to app's private executable directory, set permissions (chmod 755), and execute via root shell. |
| **bettercap Caplets** | .cap script files | Bundled in app/src/main/assets | Extract to a known location and pass the path to the bettercap binary via the \-caplet command-line argument. |
| **Python "AI"** | Python scripts | Chaquopy Gradle Plugin | Include scripts in app/src/main/python. Manage dependencies via the pip block in build.gradle. |
| **Nexmon Tools** | Native binaries (nexutil) | Pre-installed via Magisk | Execute commands using a root shell provided by libsu. The app does not bundle or install these tools. |

### **Deploying bettercap (The "Hands")**

The core of Pwnagotchi's sniffing and attack capabilities is provided by bettercap.2 The application must include and manage a compatible binary.

* **Sourcing the Binary**: A pre-compiled ARM64 Linux binary for bettercap is required. Such binaries are readily available, as they are packaged for Debian-based ARM64 systems like Kali Linux.13 The latest stable release should be downloaded for inclusion in the project.  
* **Asset Bundling and Deployment**: The bettercap binary, along with its necessary configuration scripts known as "caplets" (e.g., pwnagotchi-auto.cap), will be placed into the Android project's src/main/assets directory.9 This ensures they are packaged within the final APK.  
* **First-Run Extraction**: The PwnagotchiService must implement a first-run setup routine. On its initial start, it will check for the existence of the bettercap binary in the application's private files directory (e.g., /data/data/com.pwnagotchi.pwnagotchiandroid/files/). If the binary is not present, the service will copy it from the assets folder to this private directory. Crucially, it must then use a root shell to make the file executable by running the command: chmod 755 /data/data/com.pwnagotchi.pwnagotchiandroid/files/bettercap. This step is essential, as files in the assets directory are not directly executable.

### **Embedding the Python AI (The "Brain")**

The "learning" aspect of Pwnagotchi is driven by a set of Python scripts that interact with a machine learning model. Integrating a Python environment into an Android app can be complex, but modern tools simplify this process significantly.

* **Chaquopy Framework**: The recommended solution for this task is the Chaquopy SDK.9 Chaquopy is a Gradle plugin that fully integrates a Python interpreter into the Android build process, automating the complexities of cross-compilation and dependency management.  
* **Gradle Configuration**: The module-level build.gradle.kts file will be modified to apply the Chaquopy plugin. A python block will be added to the defaultConfig section to specify the Python version and a pip block will be used to list the required third-party packages.15 These dependencies are derived directly from the Pwnagotchi project's requirements.txt file.9 For example, a requirement like websockets==8.1 in requirements.txt translates to install("websockets==8.1") in the pip block. Certain complex packages with native components, such as tensorflow or scapy, are also supported by Chaquopy's pre-built repository, though care must be taken to ensure version compatibility.18  
* **Source Code Placement**: All Python source code for the Pwnagotchi AI will be placed in the src/main/python directory within the app module.16 Chaquopy automatically packages these scripts into the final application.

By following this strategy, the Android application becomes a self-contained package, carrying its own execution engine (bettercap) and intelligence (Python scripts). This transforms the APK from a simple remote client into a portable, deployable, and complete Pwnagotchi toolkit.

## **Hybrid Operational Mode: Leveraging a Raspberry Pi as a Dedicated Wireless Adapter**

This third operational mode provides a powerful alternative for users whose Android devices do not have a Nexmon-compatible Wi-Fi chipset but who still desire a portable, phone-driven experience. In this hybrid architecture, the Android phone acts as the "brain" and user interface, while a connected Raspberry Pi serves as a dedicated, headless wireless peripheral responsible for all hardware-level network interaction.

### **System Architecture and Communication**

The hybrid mode is defined by a clear separation of concerns between the two devices, connected physically via USB and logically over a direct network link.

* **Android Phone's Role**: The phone runs the complete Pwnagotchi Android application, including the UI, the PwnagotchiService, and the embedded Python AI engine. It offloads the actual wireless tasks to the Pi.  
* **Raspberry Pi's Role**: The Pi functions as a "dumb" wireless adapter. It is configured to run only the bettercap engine, which it exposes over the network. It does not need the full Pwnagotchi Python stack, AI model, or display drivers.  
* **Physical and Logical Connection**: The Android phone, acting as a USB host, connects to the Raspberry Pi's USB data port via a USB OTG cable.22 The Pi is configured in **USB gadget mode** (g\_ether), which makes it appear as a USB Ethernet adapter to the phone.24 This creates a direct, point-to-point IP network between the two devices, allowing for stable, high-speed communication independent of any external Wi-Fi networks.

### **Raspberry Pi Configuration as a Wireless Peripheral**

To prepare the Raspberry Pi for this role, it must be configured with a minimal OS and the necessary software to manage its Wi-Fi interface and communicate with the phone.

1. **Minimal OS and bettercap Installation**: Start with a fresh installation of Raspberry Pi OS Lite. Install bettercap and its dependencies as you would on a standard Linux system.26  
2. **Enable Monitor Mode**: Configure the Pi's built-in wlan0 interface to operate in monitor mode using standard Linux tools like airmon-ng or by installing the Nexmon DKMS modules for Raspberry Pi OS.28  
3. **Configure USB Gadget Mode**:  
   * In /boot/config.txt (or /boot/firmware/config.txt on newer OS versions), add the line: dtoverlay=dwc2.  
   * In /boot/cmdline.txt (or /boot/firmware/cmdline.txt), append modules-load=dwc2,g\_ether to the end of the line, ensuring it is separated by a space.24  
4. **Configure Networking**: Assign a static IP address to the usb0 interface that will be created by the gadget mode driver. This can be done in /etc/network/interfaces or a similar network configuration file. For example, assign the Pi the address 192.168.42.42.30  
5. **Automate bettercap Launch**: Create a systemd service to run on boot. This service should first ensure the wlan0 interface is in monitor mode, then launch bettercap with a caplet that enables its REST and WebSocket APIs, binding them to the static IP of the usb0 interface.31

### **Android Application Logic for Hybrid Mode**

The Android application must be modified to support this new mode, branching its logic to communicate with the external Pi instead of a local engine.

1. **Mode Selection**: A new option must be added to the application's settings to allow the user to select "Hybrid Mode."  
2. **Service Logic Modification**: When Hybrid Mode is active, the PwnagotchiService will:  
   * Bypass the setup for the on-device engine (it will not extract bettercap or execute nexutil commands).  
   * Use its root privileges (libsu) to configure the Android side of the USB network interface (e.g., rndis0) with a static IP in the same subnet as the Pi (e.g., 192.168.42.1).  
   * Direct its WebSocketClient to connect to the bettercap WebSocket API running on the Pi (e.g., ws://192.168.42.42:8081/events/stream).  
3. **Python AI Integration**: The Python AI, running on the phone via Chaquopy, will be instructed to send its REST API commands to the Pi's IP address instead of to localhost.

This architecture provides a robust and flexible solution, combining the processing power and rich user interface of the Android phone with the superior wireless capabilities of the Raspberry Pi, without requiring a Nexmon-compatible phone.

## **Orchestration and Inter-Process Communication (IPC)**

With the core engine components designed for multiple operational modes, the orchestration layer within the PwnagotchiService becomes responsible for launching, managing, and communicating with the appropriate processes and channeling their output to the user interface. The IPC mechanism is determined by the selected mode.

### **Redesigning PwnagotchiService as a Multi-Mode Orchestrator**

The PwnagotchiService will transition from a simple network client to a master controller.

* Its onStartCommand method will be rewritten to check the user-configured operational mode and initiate the corresponding startup sequence.  
* It will use libsu's Shell.Builder to create a single, persistent root shell for the service's lifetime, avoiding the overhead of requesting root for every command.1

### **IPC in Standalone and Hybrid Modes: The WebSocket API**

A robust and efficient method is required to channel real-time data from the running bettercap process to the Android service. The ideal solution is to leverage bettercap's built-in WebSocket API for event streaming, which provides a structured, high-performance IPC bus.3

The Pwnagotchi project's own caplet files provide the exact commands needed to enable this API 31:

\# Enable the REST API and Web UI server  
set api.rest.address 0.0.0.0  
set api.rest.port 8081  
set http.server.address 0.0.0.0  
set http.server.port 80

\# Enable the event stream over WebSocket  
events.stream on

When bettercap is launched with these commands, it starts a WebSocket server. The PwnagotchiService's existing WebSocketClient can then be reconfigured based on the operational mode:

* **Standalone Mode**: The client connects to the local endpoint: ws://127.0.0.1:8081/events/stream.33  
* **Hybrid Mode**: The client connects to the Pi's static IP address over the USB network: ws://192.168.42.42:8081/events/stream.

This elegant approach allows the application to reuse its entire existing network communication stack for both advanced modes.

### **Re-routing the Data Flow to the UI**

With the WebSocket connection established, the final step is to process the data and update the UI.

* The onMessage handler within the PwnagotchiService's WebSocketClient will receive a stream of JSON-formatted events directly from the bettercap process.  
* The structure of these events, such as wifi.handshake or wifi.ap.new, is well-defined by bettercap's API. A small translation layer within the onMessage handler will be necessary to parse these JSON objects and map their data to the fields of the existing PwnagotchiUiState data classes.  
* For example, upon receiving a wifi.handshake event from bettercap, the service will parse the relevant data, construct a Handshake object (as defined in PwnagotchiViewModel.kt), and update the \_uiState flow. Because the UI is built reactively with Jetpack Compose, this state update will automatically and efficiently trigger a re-composition, displaying the new handshake in the list without any further manual UI manipulation.1

This architecture, centered on the WebSocket as an IPC bus, effectively decouples the bettercap engine from the Android frontend, providing a clean and maintainable path for channeling real-time data to the user across different operational configurations.

## **Implementation Roadmap and Technical Recommendations**

The transformation of the application requires a phased approach that accounts for the different operational modes, careful management of privileged commands, and an awareness of potential system-level challenges.

### **Phased Implementation Plan**

A structured, step-by-step plan will mitigate risks and ensure each component is functioning correctly before full integration.

1. **Baseline (Remote UI Mode)**: Solidify and polish the application in its original state as a remote UI for a standard Pwnagotchi device. This includes fixing bugs, updating dependencies, and refining the user experience.  
2. **Standalone Mode Implementation**:  
   * **Environment Setup**: The developer must begin with a compatible Android device that has been rooted with Magisk and has a working Nexmon Magisk module installed.  
   * **Backend Preparation**: Bundle the pre-compiled ARM64 bettercap binary and Pwnagotchi caplets into the app's assets.9  
   * **Shell Orchestration**: Implement the core logic in PwnagotchiService using libsu to manage asset extraction, permissions, and the execution of nexutil and bettercap command sequences.  
   * **Python Integration**: Add the Chaquopy plugin to Gradle, translate the Pwnagotchi requirements.txt into a pip block, and place the AI source files in src/main/python.9  
3. **Hybrid Mode Implementation**:  
   * **Pi Peripheral Setup**: Prepare a Raspberry Pi by installing a minimal OS and bettercap, then configure it for monitor mode and USB gadget mode with a static IP.  
   * **Android App UI**: Add UI elements in the settings to allow users to select between Remote, Standalone, and Hybrid modes.  
   * **Service Logic Branching**: Implement logic in PwnagotchiService to switch its behavior based on the selected mode, connecting to either a local or remote bettercap instance.  
   * **Root-based Network Configuration**: Implement the code using libsu to automatically configure the Android side of the USB network when the Pi is detected.

### **Essential Shell Commands Reference**

The PwnagotchiService will be responsible for executing specific command sequences to manage the device's state in Standalone Mode.

**Table: Essential Shell Command Sequences**

| Action | Command Sequence | Source References |
| :---- | :---- | :---- |
| **Start Pwnagotchi** | 1\. svc wifi disable 2\. ifconfig wlan0 up 3\. nexutil \-m2 4\. /path/to/bettercap \-iface wlan0 \-caplet /path/to/pwnagotchi-auto.cap | 5 |
| **Stop Pwnagotchi** | 1\. killall bettercap (or use PID) 2\. nexutil \-m0 3\. ifconfig wlan0 down 4\. svc wifi enable | 34 |

### **Addressing Critical Challenges**

Deploying a system of this nature on Android involves several potential pitfalls that must be addressed proactively.

* **SELinux Policies**: Modern Android versions use SELinux in "enforcing" mode to restrict process capabilities, even for root. The bettercap binary or nexutil may be blocked from performing necessary actions. Nexmon's Magisk module often includes policies to mitigate this, sometimes by setting the system to "permissive" mode.7 If issues persist, the PwnagotchiService might need to execute setenforce 0 in its root shell at startup, though this has significant security implications and should be used as a last resort.  
* **Process Management**: The service must reliably manage the bettercap process. Simply launching the process and forgetting about it is not sufficient. The libsu library's Shell.Job API should be used to manage the running process, allowing the service to monitor its output streams (for debugging) and, most importantly, to obtain its Process ID (PID) for clean termination. Using killall bettercap is a viable but less precise alternative. The service's onDestroy method must contain a robust cleanup routine that guarantees the "Stop Pwnagotchi" command sequence is executed, ensuring the device's Wi-Fi is returned to a functional state if the app is closed or crashes.  
* **Hardware and Android Version Fragmentation**: This solution is inherently tied to specific hardware and software configurations. The report must emphasize that it will only work on devices with a supported Wi-Fi chipset, a compatible Android version, and a properly configured root environment. The application should include checks to verify these prerequisites and provide clear error messages to the user if they are not met.

A successful implementation hinges on meticulous state management. The application assumes control of a critical piece of system hardware and must be designed defensively to ensure it can always return that hardware to a normal, functional state for the user. Failure in this regard could render the device's Wi-Fi unusable without a reboot.

## **Conclusion and Future Work**

### **Summary of Feasibility, Complexity, and Dependencies**

The transformation of the "Pwnagotchi Android" application into a multi-modal, on-device tool is a highly feasible but technically complex endeavor. Its success is critically dependent on the user's hardware and willingness to prepare their device. The three operational modes present a tiered approach to functionality:

* **Remote Mode**: The simplest mode, requiring only network connectivity to a standard Pwnagotchi.  
* **Standalone Mode**: The most integrated mode, but with a hard dependency on a rooted Android device with a specific, Nexmon-compatible Wi-Fi chipset.  
* **Hybrid Mode**: A flexible middle-ground, requiring a rooted Android device with USB OTG support and a separate Raspberry Pi, but removing the dependency on specific phone hardware.

The project is therefore not a general-purpose application but a specialized toolkit for enthusiasts and security professionals with the required hardware and technical expertise.

### **Recommendations for Future Enhancements**

While the proposed architecture provides a complete and functional Pwnagotchi experience, several enhancements could significantly improve its usability, robustness, and distribution.

* **Unified Magisk Module**: The most impactful future development would be the creation of a unified Magisk module. This module would serve as a single installer that bundles all necessary components: the Nexmon firmware patch (if licensing permits redistribution), the pre-compiled bettercap ARM64 binary, all required caplets, and a complete Python environment with all dependencies from requirements.txt. This approach aligns with the installation patterns of mature mobile security platforms like Kali NetHunter.35 The Android application would then be simplified to a pure user interface, responsible only for executing commands and displaying data, with the assurance that the entire underlying environment has been correctly provisioned by the Magisk module. This would dramatically lower the barrier to entry for users.  
* **Dynamic Caplet Management**: To increase flexibility, the application could feature a caplet manager. This would allow users to view, edit, or even import their own custom bettercap caplet files directly from the app's UI. The application would save these files to its private storage and could dynamically select which caplet to load when launching the bettercap process.  
* **Handshake Management and Export**: A core function of a Pwnagotchi is the collection of WPA handshake files (.pcap).2 The bettercap engine will save these files to a directory on the device's filesystem. A future version of the application should include a file manager interface to allow users to view, manage, and easily export these captured handshakes for analysis with tools like hashcat.  
* **Expanded Hardware Support**: While Nexmon is the primary focus for standalone mode, the architecture could be made more modular to accommodate other methods of enabling monitor mode, such as supporting external USB Wi-Fi adapters through user-space drivers, similar to the approach taken by projects like liber80211.8 This would broaden the range of compatible devices.

#### **Works cited**

1. project\_backup\_full\_2025-10-16\_15-06-45.txt  
2. pwnagotchi org, accessed October 16, 2025, [https://pwnagotchi.org/](https://pwnagotchi.org/)  
3. Opwngrid \- pwnagotchi org, accessed October 16, 2025, [https://pwnagotchi.org/opwngrid/index.html](https://pwnagotchi.org/opwngrid/index.html)  
4. Monitor connectivity status and connection metering \- Android Developers, accessed October 16, 2025, [https://developer.android.com/training/monitoring-device-state/connectivity-status-type](https://developer.android.com/training/monitoring-device-state/connectivity-status-type)  
5. Flipper Zero — Portable Multi-tool Device for Geeks, accessed October 16, 2025, [https://flipperzero.one/](https://flipperzero.one/)  
6. evilsocket, accessed October 16, 2025, [https://www.evilsocket.net/](https://www.evilsocket.net/)  
7. seemoo-lab/bcm-rpi3: DEPRECATED: Monitor Mode and Firmware patching framework for the Raspberry Pi 3, development moved to: https://github.com/seemoo-lab/nexmon \- GitHub, accessed October 16, 2025, [https://github.com/seemoo-lab/bcm-rpi3](https://github.com/seemoo-lab/bcm-rpi3)  
8. evilsocket/pwngrid: ( \_ ) \- API server for pwnagotchi.ai \- GitHub, accessed October 16, 2025, [https://github.com/evilsocket/pwngrid](https://github.com/evilsocket/pwngrid)  
9. seemoo-lab/nexmon: The C-based Firmware Patching Framework for Broadcom/Cypress WiFi Chips that enables Monitor Mode, Frame Injection and much more \- GitHub, accessed October 16, 2025, [https://github.com/seemoo-lab/nexmon](https://github.com/seemoo-lab/nexmon)  
10. how to i put the wireless card on monitor mode? \- Raspberry Pi Forums, accessed October 16, 2025, [https://forums.raspberrypi.com/viewtopic.php?t=260347](https://forums.raspberrypi.com/viewtopic.php?t=260347)  
11. bettercap · GitHub Topics, accessed October 16, 2025, [https://github.com/topics/bettercap](https://github.com/topics/bettercap)  
12. How do I enable (and use) monitor mode on my wlan interface? : r/linux4noobs \- Reddit, accessed October 16, 2025, [https://www.reddit.com/r/linux4noobs/comments/149ck8/how\_do\_i\_enable\_and\_use\_monitor\_mode\_on\_my\_wlan/](https://www.reddit.com/r/linux4noobs/comments/149ck8/how_do_i_enable_and_use_monitor_mode_on_my_wlan/)  
13. kimocoder/qualcomm\_android\_monitor\_mode: Qualcomm QCACLD WiFi monitor mode for Android \- GitHub, accessed October 16, 2025, [https://github.com/kimocoder/qualcomm\_android\_monitor\_mode](https://github.com/kimocoder/qualcomm_android_monitor_mode)  
14. brycethomas/liber80211: 802.11 monitor mode for Android without root. \- GitHub, accessed October 16, 2025, [https://github.com/brycethomas/liber80211](https://github.com/brycethomas/liber80211)  
15. pwnagotchi scratch install on a Pi Zero 2W \- attempt and fail \- Reddit, accessed October 16, 2025, [https://www.reddit.com/r/pwnagotchi/comments/tc1fu8/pwnagotchi\_scratch\_install\_on\_a\_pi\_zero\_2w/](https://www.reddit.com/r/pwnagotchi/comments/tc1fu8/pwnagotchi_scratch_install_on_a_pi_zero_2w/)  
16. bettercap-caplets v20210412.r372.2d58298-4 (any) \- File List \- Arch Linux, accessed October 16, 2025, [https://archlinux.org/packages/extra/any/bettercap-caplets/files/](https://archlinux.org/packages/extra/any/bettercap-caplets/files/)  
17. Airmon-ng \- Aircrack-ng, accessed October 16, 2025, [https://www.aircrack-ng.org/doku.php?id=airmon-ng](https://www.aircrack-ng.org/doku.php?id=airmon-ng)  
18. Android CyanogenMod Kernel Building: Monitor Mode on Any Android Device with a Wireless Adapter \- Null Byte, accessed October 16, 2025, [https://null-byte.wonderhowto.com/how-to/android-cyanogenmod-kernel-building-monitor-mode-any-android-device-with-wireless-adapter-0162943/](https://null-byte.wonderhowto.com/how-to/android-cyanogenmod-kernel-building-monitor-mode-any-android-device-with-wireless-adapter-0162943/)  
19. seemoo-lab/bcm-public: DEPRECATED: Monitor Mode and Firmware patching framework for the Google Nexus 5, development moved to: https://github.com/seemoo-lab/nexmon \- GitHub, accessed October 16, 2025, [https://github.com/seemoo-lab/bcm-public](https://github.com/seemoo-lab/bcm-public)  
20. monitor-mode · GitHub Topics, accessed October 16, 2025, [https://github.com/topics/monitor-mode](https://github.com/topics/monitor-mode)  
21. Kali NetHunter Pro | Kali Linux Documentation, accessed October 16, 2025, [https://www.kali.org/docs/nethunter-pro/](https://www.kali.org/docs/nethunter-pro/)  
22. Connect Your Phone to Raspberry Pi in 15 Minutes (Even as a Beginner) \- Pidora, accessed October 16, 2025, [https://pidora.ca/connect-your-phone-to-raspberry-pi-in-15-minutes-even-as-a-beginner/](https://pidora.ca/connect-your-phone-to-raspberry-pi-in-15-minutes-even-as-a-beginner/)  
23. How to communicate between Pi 4 and pc over USB? : r/raspberry\_pi \- Reddit, accessed October 16, 2025, [https://www.reddit.com/r/raspberry\_pi/comments/10ehhrz/how\_to\_communicate\_between\_pi\_4\_and\_pc\_over\_usb/](https://www.reddit.com/r/raspberry_pi/comments/10ehhrz/how_to_communicate_between_pi_4_and_pc_over_usb/)  
24. Turning your Raspberry Pi Zero into a USB Gadget \- Adafruit Learning System, accessed October 16, 2025, [https://learn.adafruit.com/turning-your-raspberry-pi-zero-into-a-usb-gadget/ethernet-gadget](https://learn.adafruit.com/turning-your-raspberry-pi-zero-into-a-usb-gadget/ethernet-gadget)  
25. SSH the Raspberry Pi Zero over USB \- Home to artivis, accessed October 16, 2025, [https://artivis.github.io/post/2020/pi-zero/](https://artivis.github.io/post/2020/pi-zero/)  
26. Installation | bettercap, accessed October 16, 2025, [https://www.bettercap.org/project/installation/](https://www.bettercap.org/project/installation/)  
27. This is a guide to install bettercap on a Raspberry Pi \- GitHub, accessed October 16, 2025, [https://github.com/FideliusFalcon/Bettercap-Install-Raspberry-Pi-](https://github.com/FideliusFalcon/Bettercap-Install-Raspberry-Pi-)  
28. The Raspberry Pi's Wi-Fi Glow-Up | Kali Linux Blog, accessed October 16, 2025, [https://www.kali.org/blog/raspberry-pi-wi-fi-glow-up/](https://www.kali.org/blog/raspberry-pi-wi-fi-glow-up/)  
29. Monitor Mode on the Raspberry Pi Zero W, accessed October 16, 2025, [https://forums.raspberrypi.com/viewtopic.php?t=328970](https://forums.raspberrypi.com/viewtopic.php?t=328970)  
30. \[GUIDE\] Android Device as Screen for rPi via USB & VNC \- Raspberry Pi Forums, accessed October 16, 2025, [https://forums.raspberrypi.com/viewtopic.php?t=127971](https://forums.raspberrypi.com/viewtopic.php?t=127971)  
31. pwnagotchi-manual.cap ... \- GitLab, accessed October 16, 2025, [https://gitlab.com/kalilinux/packages/bettercap-caplets/-/blob/3519095890654404b81ca814755df8bbea096d38/pwnagotchi-manual.cap](https://gitlab.com/kalilinux/packages/bettercap-caplets/-/blob/3519095890654404b81ca814755df8bbea096d38/pwnagotchi-manual.cap)  
32. seemoo-lab/wisec2017\_nexmon\_jammer: This project contains the nexmon-based source code required to repeat the experiments of our WiSec 2017 paper. \- GitHub, accessed October 16, 2025, [https://github.com/seemoo-lab/wisec2017\_nexmon\_jammer](https://github.com/seemoo-lab/wisec2017_nexmon_jammer)  
33. bettercap/caplets: caplets and proxy modules. \- GitHub, accessed October 16, 2025, [https://github.com/bettercap/caplets](https://github.com/bettercap/caplets)  
34. Kali Linux 2025.3 Release (Vagrant & Nexmon), accessed October 16, 2025, [https://www.kali.org/blog/kali-linux-2025-3-release/](https://www.kali.org/blog/kali-linux-2025-3-release/)  
35. hslatman/bettercappy.ws: A small POC for using Bettercap with WebSockets in Python, accessed October 16, 2025, [https://github.com/hslatman/bettercappy.ws](https://github.com/hslatman/bettercappy.ws)