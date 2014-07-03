/**
 * Class definition.
 */
function ActivitiDialogBuilder() {
}

ActivitiDialogBuilder.prototype.buildCallActivityDialog = function(callActivity) {

	// assert activity type
	if (callActivity.activityType != "CALL_ACTIVITY") {
		throw "invalid argument exception";
	}

	// prepare the dialog template
	var dialog = getDialog(callActivity);

	// prepare the image div
	var imageDiv = $("<div id='" + callActivity.activityId + "_dialog' />");

	// finding an old Instance of this dialog and delete it
	var oldDialog = document.getElementById(callActivity.activityId + "_dialog");
	if(oldDialog != null)
	{
		oldDialog.parentNode.removeChild(oldDialog);
	}
	
	// prepare the image tag
	var image = $("<img class='map' />").attr({
		id : callActivity.activityId + "_dialog",
		border : "0",
		usemap : "#" + callActivity.activityId + "_map",
		src : getCallActivityImageUrl(callActivity)
	});

	// prepare the image map
	var map = $("<map />").attr({
		name : callActivity.activityId + "_map"
	});

	$(imageDiv).append(image);
	$(imageDiv).append(map);
	$(dialog).append(imageDiv);

	return dialog;
}

/**
 * Prepare the USER TASK activity dialog. Adds a button to the default dialog
 * template. This button is used for an user interaction with the process
 * engine.
 */
ActivitiDialogBuilder.prototype.appendUserTaskDialog = function(task, container) {

	// assert activity type
	if (task.activityType != "TASK") {
		throw "invalid argument exception";
	}

	// prepare the dialog template
	var dialog = getDialog(task);

	// prepare the complete button
	var button = $("<button class='complete' id='" + task.activityId + "_dialog'>Complete</button>");
	$(dialog).append(button);

	assertNotNull(dialog, "dialog must not be null");
	dialog.appendTo(container);
	
	// register the click event
	$(button).click(function() {
		getIt().continueUserTask(task.processId, task.activityId, function(t) {
			// do nothing
		});
	});
}

/**
 * Prepares the dialog template.
 */
function getDialog(highlightElement) {
	var rootURL = $("#rootURL").attr("value");

	var dialog = $("<div />").attr({
		id : highlightElement.activityId + "_dialog",
		title : highlightElement.activityId
	});

	var img = $("<img border='0' />").attr({
		src : getActivityPicture(highlightElement, rootURL)
	});

	var header = getDialogHeader(highlightElement);
	var span = $("<span class='dialogheader' />").text(header);

	$(dialog).append(img);
	$(dialog).append(span);
	$(dialog).append($("<hr />"));
	$(dialog).append($("<br />"));

	return dialog;
}

/**
 * Returns the URL to the CallActivity diagram image.
 * 
 * @param callActivity
 * @returns {String}
 */
function getCallActivityImageUrl(callActivity) {
	var plugin = new JenkinsActivitiPlugin();

	var segment1 = plugin.getDiagramUrl();
	var segment2 = plugin.getBuildNumber();
	var segment3 = plugin.getProcessDefinitionId();
	var segment4 = callActivity.processDescriptionId;

	return segment1 + "/" + segment2 + "/" + segment3 + "/" + segment4;
}

/**
 * Adds the CallActivity element areas to the image diagram map.
 * 
 * @param callactivity
 */
function prepareAreas(callactivity) {
	$.each(callactivity.elements, function(i, highlightElement) {
		

		var area = getArea(highlightElement);
		$(area).attr("id", callactivity.activityId + "." + $(area).attr("id"));
		
		assertNotNull(area, "area must not be null");
		area.appendTo("[name='" + callactivity.activityId + "_map']");
	});
}
