loadModule('/SIRIUS');

loadModule('/System/Resources');


# pname = "SiriusDemo2"
# project = createProject(pname)
# 
# mp = configureToModeling(project)

sample = getFile("workspace://basicfamily.sample/example.basicfamily")


vps = getViewpointsFor(sample)
for vp in vps:
    print vp.getModelFileExtension()

