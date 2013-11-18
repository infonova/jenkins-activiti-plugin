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
			return true;
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
			var area2 = new CallactivityArea("2", "RUNNING", "95", "5", "200", "60");
			var area3 = new EventArea("3", "PENDING", "273", "33", "18");
			
			var area4 = new EventArea("4", "SUCCESS", "23", "23", "18");
			var area5 = new TaskArea("5", "SUCCESS", "95", "75", "200", "130");
			var area6 = new TaskArea("6", "RUNNING", "295", "175", "400", "230");
			var area7 = new EventArea("7", "PENDING", "563", "203", "18");
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
	
	var counter = 0;
	
	it.getTaskStates = function(processDescriptionId, buildNr, callback) {
		var response = new Object();
		response.responseObject = function() {
			
			var map = new Map();
			if (counter > 10) {
				map["1"] = "SUCCESS";
				map["2"] = "SUCCESS";
				map["3"] = "SUCCESS";
				map["2.4"] = "SUCCESS";
				map["2.5"] = "SUCCESS";
				map["2.6"] = "SUCCESS";
				map["2.7"] = "SUCCESS";
			}
			else {
				map["1"] = "SUCCESS";
				map["2"] = "RUNNING";
				map["3"] = "PENDING";
				map["2.4"] = "SUCCESS";
				map["2.5"] = "SUCCESS";
				map["2.6"] = "RUNNING";
				map["2.7"] = "PENDING";
			}			
counter ++;

			return map;
		}
		callback(response);
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
