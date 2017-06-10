loadModule('/SIRIUS');

loadModule('/System/Resources');


sample = getFile("workspace://basicfamily.sample/example.basicfamily")

project = createProject("SiriusDemo")

copyFile(sample, "workspace://SiriusDemo/example.basicfamily")
target = getFile("workspace://SiriusDemo/example.basicfamily")

session = createSession(target)

# representation = createRepresentation(target, session)

