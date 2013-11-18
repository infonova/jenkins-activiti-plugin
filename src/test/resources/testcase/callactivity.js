// TODO: use JsMockito

$(function() {
	var it = new Object();

	// mock getErrorAction function
	it.getErrorAction = function(buildNr, callback) {
		var response = new Object();
		response.responseObject = nullAnswer;
		callback(response);
	}

	// mock isBuilding function
	it.isBuilding = function(buildNr, callback) {
		var response = new Object();
		response.responseObject = function() {
			return false;
		};
		callback(response);
	}

	// mock getProcessDefinitionIds function
	it.getProcessDefinitionIds = function(buildNr, callback) {
		var response = new Object();
		response.responseObject = function() {
			var action1 = new WorkflowAction("id1");
			var action2 = new WorkflowAction("id2");
			var action3 = new WorkflowAction("id3");
			return [ action1, action2, action3 ];
		}
		callback(response);
	}

	// mock getHighlightElements function
	it.getHighlightElements = function(processDescriptionId, buildNr, callback) {
		var response = new Object();
		response.responseObject = function() {
			var area1 = new EventArea("1", "SUCCESS", "23", "33", "18");
			var area2 = new CallactivityArea("2", "SUCCESS", "95", "5", "200", "60");
			var area3 = new EventArea("3", "SUCCESS", "273", "33", "18");
			
			var area4 = new EventArea("4", "SUCCESS", "23", "23", "18");
			var area5 = new TaskArea("5", "SUCCESS", "95", "75", "200", "130");
			var area6 = new TaskArea("6", "SUCCESS", "295", "175", "400", "230");
			var area7 = new EventArea("7", "SUCCESS", "563", "203", "18");
            area2.elements.push(area4);
            area2.elements.push(area5);
            area2.elements.push(area6);
            area2.elements.push(area7);
			
			return [ area1, area2, area3 ];
		}
		callback(response);
	}
	
	// mock async function
	it.async = function(callback) {
		callback();
	}

	JenkinsActivitiPlugin.prototype.getImageURL = function() {
		var plugin = new JenkinsActivitiPlugin();
		return plugin.getDiagramUrl() + "/" + plugin.getProcessDefinitionId() + ".png";
	}

	JenkinsActivitiPlugin.prototype.getDiagramUrl = function() {
		return "./plugin/jenkins-activiti-plugin/image/";
	}

	JenkinsActivitiPlugin.prototype.it = it;
});
