-- MySQL dump 8.22
--
-- Host: localhost    Database: tempcopy
---------------------------------------------------------
-- Server version	3.23.51-nt-log

--
-- Dumping data for table 'alib_component'
--


LOCK TABLES alib_component WRITE;
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('Completion Event Insertion','Completion Event Insertion','recipe|##RECIPE_CLASS##','recipe',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('Completion Event Insertion|org.cougaar.glm.plugin.completion.GLMCompletionNodePlugin','Completion Event Insertion|org.cougaar.glm.plugin.completion.GLMCompletionNodePlugin','plugin|org.cougaar.glm.plugin.completion.GLMCompletionNodePlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('Completion Event Insertion|org.cougaar.glm.plugin.completion.GLMCompletionSocietyPlugin','Completion Event Insertion|org.cougaar.glm.plugin.completion.GLMCompletionSocietyPlugin','plugin|org.cougaar.glm.plugin.completion.GLMCompletionSocietyPlugin','plugin',0.000000000000000000000000000000);
REPLACE INTO alib_component (COMPONENT_ALIB_ID, COMPONENT_NAME, COMPONENT_LIB_ID, COMPONENT_TYPE, CLONE_SET_ID) VALUES ('Completion Event Insertion|org.cougaar.glm.plugin.completion.GLMCompletionTargetPlugin','Completion Event Insertion|org.cougaar.glm.plugin.completion.GLMCompletionTargetPlugin','plugin|org.cougaar.glm.plugin.completion.GLMCompletionTargetPlugin','plugin',0.000000000000000000000000000000);
UNLOCK TABLES;

--
-- Dumping data for table 'asb_agent'
--


LOCK TABLES asb_agent WRITE;
UNLOCK TABLES;

--
-- Dumping data for table 'asb_agent_pg_attr'
--


LOCK TABLES asb_agent_pg_attr WRITE;
UNLOCK TABLES;

--
-- Dumping data for table 'asb_agent_relation'
--


LOCK TABLES asb_agent_relation WRITE;
UNLOCK TABLES;

--
-- Dumping data for table 'asb_assembly'
--


LOCK TABLES asb_assembly WRITE;
INSERT INTO asb_assembly (ASSEMBLY_ID, ASSEMBLY_TYPE, DESCRIPTION) VALUES ('RCP-0002-Completion Event Insertion','RCP','Completion Insertion [1]');
UNLOCK TABLES;

--
-- Dumping data for table 'asb_component_arg'
--


LOCK TABLES asb_component_arg WRITE;
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0002-Completion Event Insertion','Completion Event Insertion|org.cougaar.glm.plugin.completion.GLMCompletionNodePlugin','UPDATE_INTERVAL=30000',1.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0002-Completion Event Insertion','Completion Event Insertion|org.cougaar.glm.plugin.completion.GLMCompletionSocietyPlugin','DO_PERSISTENCE=false',1.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0002-Completion Event Insertion','Completion Event Insertion|org.cougaar.glm.plugin.completion.GLMCompletionSocietyPlugin','INITIAL_TIME_ADVANCE_DELAY=180000',4.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0002-Completion Event Insertion','Completion Event Insertion|org.cougaar.glm.plugin.completion.GLMCompletionSocietyPlugin','NORMAL_TIME_ADVANCE_DELAY=120000',3.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0002-Completion Event Insertion','Completion Event Insertion|org.cougaar.glm.plugin.completion.GLMCompletionSocietyPlugin','UPDATE_INTERVAL=30000',2.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0002-Completion Event Insertion','Completion Event Insertion|org.cougaar.glm.plugin.completion.GLMCompletionTargetPlugin','ACTIVITY_DELAY=1200000',2.000000000000000000000000000000);
INSERT INTO asb_component_arg (ASSEMBLY_ID, COMPONENT_ALIB_ID, ARGUMENT, ARGUMENT_ORDER) VALUES ('RCP-0002-Completion Event Insertion','Completion Event Insertion|org.cougaar.glm.plugin.completion.GLMCompletionTargetPlugin','SLEEP_INTERVAL=30000',1.000000000000000000000000000000);
UNLOCK TABLES;

--
-- Dumping data for table 'asb_component_hierarchy'
--


LOCK TABLES asb_component_hierarchy WRITE;
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0002-Completion Event Insertion','Completion Event Insertion|org.cougaar.glm.plugin.completion.GLMCompletionNodePlugin','Completion Event Insertion','COMPONENT',1.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0002-Completion Event Insertion','Completion Event Insertion|org.cougaar.glm.plugin.completion.GLMCompletionSocietyPlugin','Completion Event Insertion','COMPONENT',0.000000000000000000000000000000);
INSERT INTO asb_component_hierarchy (ASSEMBLY_ID, COMPONENT_ALIB_ID, PARENT_COMPONENT_ALIB_ID, PRIORITY, INSERTION_ORDER) VALUES ('RCP-0002-Completion Event Insertion','Completion Event Insertion|org.cougaar.glm.plugin.completion.GLMCompletionTargetPlugin','Completion Event Insertion','COMPONENT',2.000000000000000000000000000000);
UNLOCK TABLES;

--
-- Dumping data for table 'asb_oplan'
--


LOCK TABLES asb_oplan WRITE;
UNLOCK TABLES;

--
-- Dumping data for table 'asb_oplan_agent_attr'
--


LOCK TABLES asb_oplan_agent_attr WRITE;
UNLOCK TABLES;

--
-- Dumping data for table 'community_attribute'
--


LOCK TABLES community_attribute WRITE;
UNLOCK TABLES;

--
-- Dumping data for table 'community_entity_attribute'
--


LOCK TABLES community_entity_attribute WRITE;
UNLOCK TABLES;

--
-- Dumping data for table 'lib_agent_org'
--


LOCK TABLES lib_agent_org WRITE;
UNLOCK TABLES;

--
-- Dumping data for table 'lib_component'
--


LOCK TABLES lib_component WRITE;
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('recipe|##RECIPE_CLASS##','recipe','##RECIPE_CLASS##','recipe','Added recipe');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.glm.plugin.completion.GLMCompletionNodePlugin','plugin','org.cougaar.glm.plugin.completion.GLMCompletionNodePlugin','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.glm.plugin.completion.GLMCompletionSocietyPlugin','plugin','org.cougaar.glm.plugin.completion.GLMCompletionSocietyPlugin','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
REPLACE INTO lib_component (COMPONENT_LIB_ID, COMPONENT_TYPE, COMPONENT_CLASS, INSERTION_POINT, DESCRIPTION) VALUES ('plugin|org.cougaar.glm.plugin.completion.GLMCompletionTargetPlugin','plugin','org.cougaar.glm.plugin.completion.GLMCompletionTargetPlugin','Node.AgentManager.Agent.PluginManager.Plugin','Added plugin');
UNLOCK TABLES;

--
-- Dumping data for table 'lib_mod_recipe'
--


LOCK TABLES lib_mod_recipe WRITE;
REPLACE INTO lib_mod_recipe (MOD_RECIPE_LIB_ID, NAME, JAVA_CLASS, DESCRIPTION) VALUES ('RECIPE-0005Completion Event Insertion','Completion Event Insertion','org.cougaar.tools.csmart.recipe.ComponentCollectionRecipe','No description available');
UNLOCK TABLES;

--
-- Dumping data for table 'lib_mod_recipe_arg'
--


LOCK TABLES lib_mod_recipe_arg WRITE;
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0005Completion Event Insertion','$$CP=org.cougaar.glm.plugin.completion.GLMCompletionNodePlugin-1',3.000000000000000000000000000000,'recipeQueryAllNodes');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0005Completion Event Insertion','$$CP=org.cougaar.glm.plugin.completion.GLMCompletionSocietyPlugin-0',4.000000000000000000000000000000,'recipeQueryNCAAgent');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0005Completion Event Insertion','$$CP=org.cougaar.glm.plugin.completion.GLMCompletionTargetPlugin-2',2.000000000000000000000000000000,'recipeQueryAllAgents');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0005Completion Event Insertion','Assembly Id',0.000000000000000000000000000000,'RCP-0002-Completion Event Insertion');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-0005Completion Event Insertion','Target Component Selection Query',1.000000000000000000000000000000,'recipeQuerySelectNothing');
UNLOCK TABLES;

--
-- Dumping data for table 'lib_pg_attribute'
--


LOCK TABLES lib_pg_attribute WRITE;
UNLOCK TABLES;

