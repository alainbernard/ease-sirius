loadModule('/SIRIUS')
loadModule('/System/Resources')

# Get the Sirius example EMF resource
sample = getFile("workspace://basicfamily.sample/example.basicfamily")

# Create a new project in workspace
pname = "SiriusDemo2"
project = createProject(pname)

# Put the example file in this new project
targetPath = "workspace://"+pname+"/example.basicfamily"
copyFile(sample, targetPath)
target = getFile(targetPath)

# Start the Sirius session
session = createSession(target)
