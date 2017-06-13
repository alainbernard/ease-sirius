loadModule('/SIRIUS');

loadModule('/System/Resources');

sample = getFile("workspace://basicfamily.sample/example.basicfamily")

pname = "SiriusDemo"
project = createProject(pname)

targetPath = "workspace://"+pname+"/example.basicfamily"
copyFile(sample, targetPath)
target = getFile(targetPath)

session = createSession(target)

repr = getRepresentation(session)
print repr
