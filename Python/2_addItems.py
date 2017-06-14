loadModule('/SIRIUS')
loadModule('/System/Resources')

# Get the example project created with previous script
project = getProject("SiriusDemo")

session = loadExistingSession(project)
repr = getRepresentation(session)

# Load the root object of the model
family = getModelRoot(session, repr, "basicfamily")

# Create a new Man instance and give him a name
man = invokeAdd(session, family, "members", "Man")
invokeSet(session, man, "name", "James")
 
# Add this man as the child of someone
print "Link the created element to " + family.getMembers().get(0).getName()
invokeSet(session, man, "parents", family.getMembers().get(0))
