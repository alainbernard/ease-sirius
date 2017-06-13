loadModule('/SIRIUS');
    
loadModule('/System/Resources');

project = getProject("SiriusDemo")
   
session = loadExistingSession(project)
   
print session
   
repr = getRepresentation(session)
    
print repr
    
invokeTool(session, repr)