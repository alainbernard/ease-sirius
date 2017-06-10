# Script Eclipse Sirius with EASE

This project features the scripting of Eclipse SIRIUS with EASE - Eclipse Advanced Scripting Environment. 

The examples are based on the "basicfamily" samples bundled with Sirius. 

The target platform contained in /tp folder gives the minimal target platform required to run an Eclipse App with EASE, Sirius and the provided module. 

## How to run 

- Import the "org.eclipse.sirius.ease" plugin into your workspace.
- Set the target platform using the .target file located in the /tp folder of the plugin
- Import the Sirius samples in your workspace: "Family metamodel definition", and "Sirius Modeler definition - Advanced tutorial"
- Modify the "design" file to set "initialization" to true on the Persons diagram

In the runtime application:
 
- Import the Basic Family sample model
- Import and Run as > Ease Script the Python file located in the sirius-ease plugin in /python folder
