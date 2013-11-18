$(function() {
	JenkinsActivitiPlugin.prototype.building = true;

	callbackErrorAction(initDiagramImage, displayError);

	displayContinuous();

	// events
	registerBuildEvent();
	registerWorkflowOptionEvent();
	registerImageDiagramEvent();
});

/**
 * Registers a click event for all elements with a build class.
 */
function registerBuildEvent() {
	$(".build").click(function() {
		JenkinsActivitiPlugin.prototype.building = true;

		// update hidden text fields
		setBuildNumber(this.id);

		cleanupUI();
		callbackErrorAction(function() {
			updateWorkflowDropdown(getBuildNumber(), true);
		}, displayError);
	});
}

// TODO: documentation
function cleanupUI() {
	$("#errors").empty();
}

/**
 * Initializes the click event for all workflow select options. Updates the view
 * when triggered.
 */
function registerWorkflowOptionEvent() {
	$("#workflowselect").change(function() {
		cleanupUI();

		var option = $("#workflowselect option:selected");
		var processDefinitionId = $(option).attr("id");

		updateView(getBuildNumber(), processDefinitionId);
	});
}

/**
 * Sets the build number.
 * 
 * @param buildNumber
 */
function setBuildNumber(buildNumber) {
	$("#buildNumber").attr("value", buildNumber);
	$("#displayedBuildNr").html("(" + buildNumber + ")");
}

/**
 * Returns the build number.
 * 
 * @return int
 */
function getBuildNumber() {
	return $("#buildNumber").attr("value");
}

/**
 * Sets the root url.
 * 
 * @param rootUrl
 */
function setRootUrl(rootUrl) {
	$("#rootURL").attr("value", buildNumber);
}

/**
 * Returns the root URL
 * 
 * @return string
 */
function getRootURL() {
	return $("#rootURL").attr("value");
}

/**
 * Sets the process definition id.
 * 
 * @param processDefinitionId
 */
function setProcessDefinitionId(processDefinitionId) {
	$("#processDefinitionId").attr("value", processDefinitionId);
}

/**
 * Returns the process definition id.
 * 
 * @return String
 */
function getProcessDefinitionId() {
	return $("#processDefinitionId").attr("value");
}

/**
 * Returns the process name.
 * 
 * @return String
 */
function getProjectName() {
	return $("#projectName").attr("value");
}

/**
 * Initialized the load event on the diagram image element.
 */
function registerImageDiagramEvent() {
	$("#diagram").load(function() {
		renderHilighting();
		initializeMapHighlights();
	});
}

// documentation
function updateView(buildNumber, processDefinitionId) {
	var rootURL = $("#rootURL").attr("value");

	setProcessDefinitionId(processDefinitionId);
	setBuildNumber(buildNumber);

	// TODO: move to JenkinsActivitiPlugin
	$("#displayedBuildNr").attr("href",
			rootURL + "/job/" + getProjectName() + "/" + buildNumber);

	callbackErrorAction(function() {
		var plugin = new JenkinsActivitiPlugin();
		changeDiagramImage(plugin.getImageURL());
	}, displayError);
}

/**
 * Changes the diagram image source. Triggers a load event.
 */
function changeDiagramImage(imageURL) {
	$("#diagram").one("load", function() {
		renderHilighting();
		initializeMapHighlights();
	}).attr("src", imageURL);
}

/**
 * Initializes the JQuery UI dialogs.
 * 
 * Each dialog is configured in a modal mode. In addition to that the dialog
 * only opens when a element with the class 'opener' is clicked.
 * 
 */
function initializeDialogs() {
	$(".dialog").dialog({
		resizable : false,
		height : 140,
		width : "auto",
		modal : true,
		autoOpen : false,
		show : {
			effect : "blind",
			duration : 1000
		},
		hide : {
			effect : "explode",
			duration : 1000
		}
	});
}

/**
 * Initializes the map highlighting. Configures the map highlight properties.
 */
function initializeMapHighlights() {
	$('.map').maphilight({
		alwaysOn : true,
		stroke : false,
		fillOpacity : 0.5
	});
}

/**
 * Initializes the diagram image. Renders the latest build diagram.
 * 
 * @deprecated use changeDiagramImage
 */
function initDiagramImage() {
	var plugin = new JenkinsActivitiPlugin();
	$("#diagram").attr("src", plugin.getImageURL());
}

/**
 * Loads the element coordinates and renders the highlight elements.
 */
function renderHilighting() {
	$("[name='map']").empty();

	var processDefinitionId = getProcessDefinitionId();
	var buildNumber = getBuildNumber();

	assertNotUndefined(processDefinitionId, "process definition is undefined");
	assertNotUndefined(buildNumber, "build number is undefined");

	getIt().getHighlightElements(processDefinitionId, buildNumber, function(t) {
		var highlightElements = t.responseObject();

		$.each(highlightElements, function(i, highlightElement) {
			var area = getArea(highlightElement);
			assertNotNull(area, "area must not be null");
			area.appendTo("[name='map']");

			var type = highlightElement.activityType;

			// SUB PROCESS ACTIVITY
			if (type == "CALL_ACTIVITY") {
				var builder = new ActivitiDialogBuilder();
				var dialog = builder.buildCallActivityDialog(highlightElement);
				assertNotNull(dialog, "dialog must not be null");
				dialog.appendTo("#dialogwrapper");
				prepareAreas(highlightElement);
				initializeMapHighlights();
			}

			// TASK Activity
			else if (type == "TASK") {

				var taskType = highlightElement.taskType;

				// prepare the user task dialog
				if (taskType == "USER_TASK") {
					var builder = new ActivitiDialogBuilder();
					builder.appendUserTaskDialog(highlightElement, "#dialogwrapper");
				} 
				
				// prepare the task dialog
				else {
					var dialog = getDialog(highlightElement);
					assertNotNull(dialog, "dialog must not be null");
					dialog.appendTo("#dialogwrapper");
				}
				
			}

			// DEFAULT Activity
			else {
				var dialog = getDialog(highlightElement);
				assertNotNull(dialog, "dialog must not be null");
				dialog.appendTo("#dialogwrapper");
			}

		});

		// register click event
		$(".opener").click(function() {
			var dialog = $("#" + this.id + "_dialog");
			dialog.dialog("open");
		});

		display();
		async(initializeDialogs);
	});
}

/**
 * Calls the given function asynchronously.
 */
function async(_function) {
	getIt().async(function(t) {
		_function();
	});
}

/**
 * Loads the task states and updates the highlight state.
 */
function updateStates() {
	var processDefinitionId = getProcessDefinitionId();
	var buildNumber = $("#buildNumber").attr("value");

	assertNotUndefined(processDefinitionId, "process definition is undefined");
	assertNotUndefined(buildNumber, "build number is undefined");

	getIt().getTaskStates(processDefinitionId, buildNumber, function(t) {
		var states = t.responseObject();

		$('area').each(function() {
			var state = states[this.id];
			assertNotUndefined(state, "no state found for id " + this.id);
			$(this).attr("name", state.toLowerCase());
		});
	});
}

/**
 * Asserts if the given object is not undefined.
 */
function assertNotUndefined(obj, msg) {
	if (obj == undefined) {
		throw msg;
	}
}

/**
 * Asserts if the given object is not null.
 */
function assertNotNull(obj, msg) {
	if (obj == null) {
		throw msg;
	}
}

/**
 * Returns the it instance.
 * 
 * @returns it
 */
function getIt() {
	return JenkinsActivitiPlugin.prototype.it;
}

// TODO: documentation
function callbackErrorAction(success, failure) {
	JenkinsActivitiPlugin.prototype.hasError = undefined;

	getIt().getErrorAction(getBuildNumber(), function(t) {
		var error = t.responseObject();
		JenkinsActivitiPlugin.prototype.hasError = (error != undefined);

		if (error != undefined) {
			failure(error);
		} else {
			success();
		}
	});
}

/**
 * Returns a new area element with the given highlight element attributes.
 * 
 * The area element has a 'opener' class in order to be available to open
 * dialogs by the use of the JQuery UI library.
 * 
 * @param highlightElement
 * @returns area element
 */
function getArea(highlightElement) {

	return $('<area/>').attr({
		id : highlightElement.activityId,
		name : highlightElement.state.toLowerCase(),
		shape : highlightElement.shape.toLowerCase(),
		coords : highlightElement.coordinates,
		href : prepareLink(highlightElement),
		class : getAreaClass(highlightElement)
	});
}

/**
 * Returns the html class for the given highlight element.
 * 
 * @param highlightElement
 */
function getAreaClass(highlightElement) {
	var link = highlightElement.link;
	return link.indexOf("#") == 0 ? "opener" : "";
}

/**
 * Builds a dialog element with the information of the given highlight element.
 * The dialog will be displayed on the user interface by the use of the JQuery
 * UI library.
 * 
 * Therefore the title attribute have to be set in order to display a header
 * text.
 * 
 * The dialog displays common information about the selected BPMN element.
 * 
 * @param highlightElement
 * @returns a dialog element
 */
function getDialog(highlightElement) {
	var rootURL = $("#rootURL").attr("value");

	var dialog = $("<div class='dialog' />").attr({
		id : highlightElement.activityId + "_dialog",
		title : highlightElement.activityId
	});

	var img = $("<img border='0' />").attr({
		src : getActivityPicture(highlightElement, rootURL)
	});

	var span = $("<span class='dialogheader' />").text(
			getDialogHeader(highlightElement));

	$(dialog).append(img);
	$(dialog).append(span);
	$(dialog).append($("<hr />"));
	$(dialog).append($("<br />"));

	return dialog;
}

/**
 * Builds a dialog element with the error information. The dialog will be
 * displayed on the user interface by the use of the JQuery UI library.
 * 
 * The dialog displays information about the occurred error.
 * 
 * @param error
 * @returns a dialog element
 */
function getErrorDialog(error) {

	var dialog = $("<div class='dialog' />").attr({
		id : error.errorCode + "_dialog",
		title : "Error code: " + error.errorCode
	});

	var errorMessage = $("<span />").attr({
		id : error.errorCode + "_errormessage",
	});

	var plugin = new JenkinsActivitiPlugin();
	var url = plugin.getErrorMessageURL(error);

	$.get(url, function(data) {
		$(errorMessage).html(data);
	});

	$(dialog).append(errorMessage);

	return dialog;
}

/**
 * Returns the activity type string of the given highlight element for the
 * dialog header representation text.
 * 
 * @param highlightElement
 * @returns {String}
 */
function getDialogHeader(highlightElement) {
	var type = highlightElement.activityType;
	return type.substring(0, 1) + type.substring(1).toLowerCase();
}

/**
 * Returns the icon file name of the given highlight element.
 * 
 * @param highlightElement
 * @param rootURL
 * @returns {String}
 */
function getActivityPicture(highlightElement, rootURL) {
	var type = highlightElement.activityType.toLowerCase();
	// TODO: remove double slash
	return rootURL + "/plugin/jenkins-activiti-plugin/"
			+ highlightElement.iconFileName;
}

// TODO: documentation
function getEmptyPicture() {
	var rootURL = $("#rootURL").attr("value");
	return rootURL + "/plugin/jenkins-activiti-plugin/image/Empty.png";
}

/**
 * Prepares the given link for highlight element href attribute.
 * 
 * If the given link starts with an slash the returned link will be relative to
 * the root URL.
 * 
 * In other case the given link will be returned unmodified.
 * 
 * @param link
 * @returns the prepared link
 */
function prepareLink(highlightElement) {
	var link = highlightElement.link;

	if (link.indexOf("/") == 0) {
		var rootURL = $("#rootURL").attr("value");

		var jobName = highlightElement.jobName;
		var processDefinitionId = getProcessDefinitionId();

		assertNotUndefined(processDefinitionId, "process def is undefined");
		assertNotUndefined(jobName, "job name is undefined");

		getIt().getBuildNumber(processDefinitionId, jobName, function(t) {
			var buildNr = t.responseObject();

			if (buildNr != undefined) {
				var area = $("#" + highlightElement.activityId);
				var href = $(area).attr("href");
				$(area).attr("href", href + "/" + buildNr);
			}
		});

		return rootURL + link;
	}

	return link;
}

// documentation
function display() {

	if (JenkinsActivitiPlugin.prototype.building) {
		updateStates();
	}
	var bool = JenkinsActivitiPlugin.prototype.displayed;

	renderSuccessArea();
	renderFailureArea();
	renderRunningArea(bool);

	JenkinsActivitiPlugin.prototype.displayed = !bool;
}

// documentation
function renderArea(_class, alwaysOn, fillColor) {
	$('[name="' + _class + '"]').each(function() {
		var data = $(this).data('maphilight') || {};
		data.fillColor = fillColor;
		data.alwaysOn = alwaysOn;
		$(this).data('maphilight', data).trigger('alwaysOn.maphilight');
	});
}

/**
 * Renders all area elements which are in success state.
 */
function renderSuccessArea() {
	renderArea("success", true, "268f09");
}

/**
 * Renders all area elements which are in failure state.
 */
function renderFailureArea() {
	renderArea("failure", true, "ff0000");
}

/**
 * Renders all area elements which are in running state.
 */
function renderRunningArea(bool) {
	renderArea("running", bool, "333333");
}

function displayBuild() {

}

function displayError(error) {
	// $("[name='map']").empty();
	changeDiagramImage(getEmptyPicture());

	var div = $("#errors");

	var h3 = $("<h3/>").html("Error code: " + error.errorCode);
	var anchor = $("<a/>").html("Error message").attr({
		href : "#" + error.errorCode
	});

	var dialog = getErrorDialog(error);
	dialog.appendTo("#dialogwrapper");

	$(anchor).click(function() {
		$("#" + error.errorCode + "_dialog").dialog("open");
	});

	$(div).append(h3);
	$(div).append(anchor);

	async(initializeDialogs);
}

// TODO: documentation
function displayContinuous() {

	var plugin = new JenkinsActivitiPlugin();

	if (plugin.isBuilding() && !plugin.hasError) {
		var buildNumber = getBuildNumber();

		assertNotUndefined(buildNumber, "build number is undefined");

		getIt().isBuilding(buildNumber, function(t) {
			var building = t.responseObject();
			JenkinsActivitiPlugin.prototype.building = building;

			if (building) {
				display();
			} else {
				renderSuccessArea();
				renderFailureArea();
				renderRunningArea(true);
			}

			updateWorkflowDropdown(buildNumber, false);
		});

	}
	setTimeout(displayContinuous, "1000");
}

// TODO: documentation
function updateWorkflowDropdown(buildNr, init) {
	assertNotUndefined(buildNr, "build number is undefined");

	getIt().getProcessDefinitionIds(buildNr, function(t) {
		var workflows = t.responseObject();

		if (init) {
			// update the hidden process definition id input element
			var id = workflows[0].processDescriptionId;
			setProcessDefinitionId(id);

			// rebuild the workflow select element
			$("#workflowselect").empty();
			$.each(workflows, function(i, workflow) {
				var option = createWorkflowOption(workflow);
				option.appendTo("#workflowselect");
			});

			updateView(buildNr, id);
		} else {
			$.each(workflows, function(i, workflow) {

				// check if option already exists
				var processId = workflow.processDescriptionId;
				var id = "#" + processId.replace(/([:])/g, "\\$1");

				if ($(id).attr("id") == undefined) {
					var option = createWorkflowOption(workflow);
					option.appendTo("#workflowselect");
				} else {
					// update finished workflow state
					$(id).attr("class", workflow.workflowState);
				}
			});
		}

	});
}

/**
 * Returns a new <option/> element for workflow represenation in the workflow
 * select element.
 * 
 * @param workflow
 */
function createWorkflowOption(workflow) {
	var option = $("<option name='workflow' />").attr({
		class : workflow.workflowState,
		id : workflow.processDescriptionId
	});
	$(option).html(workflow.workflowName);
	return option;
}

/**
 * ********************************************************* Jenkins activiti
 * plugin object. *********************************************************
 */
function JenkinsActivitiPlugin() {
}

JenkinsActivitiPlugin.prototype.displayed = true;

JenkinsActivitiPlugin.prototype.hasError = undefined;

JenkinsActivitiPlugin.prototype.building = undefined;

/**
 * Returns the image URL by the given base URL, project name and build number.
 */
JenkinsActivitiPlugin.prototype.getImageURL = function() {

	var plugin = new JenkinsActivitiPlugin();
	var segment1 = plugin.getDiagramUrl();
	var segment2 = plugin.getBuildNumber();
	var segment3 = plugin.getProcessDefinitionId();

	return segment1 + "/" + segment2 + "/" + segment3;
}

/**
 * Returns the image URL by the given base URL, project name and build number.
 */
JenkinsActivitiPlugin.prototype.getDiagramUrl = function() {

	var plugin = new JenkinsActivitiPlugin();

	var rootURL = plugin.getRootUrl();
	var baseURL = plugin.getBaseURL(rootURL);

	return baseURL += "/job/" + getProjectName() + "/diagram/diagram/";
}

JenkinsActivitiPlugin.prototype.getRootUrl = function() {
	return $("#rootURL").attr("value");
}

JenkinsActivitiPlugin.prototype.getBuildNumber = function() {
	return $("#buildNumber").attr("value");
}

JenkinsActivitiPlugin.prototype.getProcessDefinitionId = function() {
	return getProcessDefinitionId();
}

/**
 * Indicates if the build is currently running.
 * 
 * @return boolean
 */
JenkinsActivitiPlugin.prototype.isBuilding = function() {
	return JenkinsActivitiPlugin.prototype.building;
}

/**
 * Sets the building value.
 * 
 * @param building
 */
JenkinsActivitiPlugin.prototype.setBuilding = function(building) {
	JenkinsActivitiPlugin.prototype.building = building;
}

/**
 * Returns the error message URL by the given base URL.
 */
JenkinsActivitiPlugin.prototype.getErrorMessageURL = function(error) {
	var rootURL = $("#rootURL").attr("value");
	var buildNumber = $("#buildNumber").attr("value");

	var plugin = new JenkinsActivitiPlugin();
	var baseURL = plugin.getBaseURL(rootURL);

	return baseURL += "/job/" + getProjectName() + "/error/errorMessage/"
			+ buildNumber + "/" + error.errorRef;
}

/**
 * Returns the base URL by the given root URL.
 * 
 * @param rootURL
 */
JenkinsActivitiPlugin.prototype.getBaseURL = function(rootURL) {
	var location = window.location.href;
	var baseURL = location.substr(0, location.indexOf(rootURL + "/"));

	return baseURL + rootURL;
}
