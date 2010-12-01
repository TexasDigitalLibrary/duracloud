/**
 * 
 * created by Daniel Bernstein
 */

var centerLayout;
var servicesListPane;
var detailPane;


$(function() {

	
	//alert("starting jquery execution");
	var serviceDetailPaneId = "#service-detail-pane";
	var detailPaneId = "#detail-pane";
	var servicesListViewId = "#services-list-view";
	var servicesListId = "#services-list";
	
	centerLayout = $('#page-content').layout({
		west__size:				800
	,   west__paneSelector:     servicesListViewId
	,   west__onresize:         "servicesListPane.resizeAll"
	,	center__paneSelector:	detailPaneId
	,   center__onresize:       "detailPane.resizeAll"
	});
	
	
	
	var servicesListPaneLayout = {
			north__paneSelector:	".north"
		,   north__size: 			35
		,	center__paneSelector:	".center"
		,   resizable: 				false
		,   slidable: 				false
		,   spacing_open:			0			
		,	togglerLength_open:		0	
	};
			
	servicesListPane = $(servicesListViewId).layout(servicesListPaneLayout);
	
	//detail pane's layout options
	var detailLayoutOptions = {
				north__paneSelector:	".north"
				,   north__size: 			200
				,	center__paneSelector:	".center"
				,   resizable: 				false
				,   slidable: 				false
				,   spacing_open:			0
				,	togglerLength_open:		0
				
	};
	
	detailPane = $(detailPaneId).layout(detailLayoutOptions);
	
	var resolveServiceCssClass = function(services){
		return "service-replicate";
	};

	
	var loadDeploymentDetail = function(service,deployment){
		
		if(service == null){
			
			$(detailPaneId).fadeOut("slow", function(){
				$(this).html('');
			});
			return;
		};
		var serviceDetailPane = $(serviceDetailPaneId).clone();

		
		//set the title of the pane
		$(".service-name", serviceDetailPane.first()).html(service.displayName);
		$(".service-version", serviceDetailPane.first()).html(service.serviceVersion);
		$("#deployment-id", serviceDetailPane.first()).val(deriveDeploymentId(service,deployment));
		
		var centerPane = $(".center",serviceDetailPane.first());
		centerPane.html("");

		//deploy/undeploy switch definition and bindings
		$(".deploy-switch",serviceDetailPane.first()).onoffswitch({
			   		initialState: "on"
								, onStateClass: "on left"
								, onIconClass: "checkbox"
								, offStateClass: "off right"
								, offIconClass: "x"
								, onText: "Deploy"
								, offText: "Undeploy"
		}).bind("turnOff", function(evt, future){
			
			var callback = {
					begin: function(){
						dc.busy("Undeploying " + service.displayName);
					},
					
					success: function(){
						dc.done();
						$(servicesListId).selectablelist("removeById", deriveDeploymentId(service,deployment));
						future.success();
					},
					
					failure: function(text){
						dc.done();
						alert("failed to undeploy service!");
						if(future.failure != undefined){
							future.failure(text);
						}
					}
			};

			dc.service.Undeploy(service, deployment,callback);
		});
		
		$(".reconfigure-button",serviceDetailPane.first()).click(function(){
			$("#reconfigure-service-dialog").dialog("open");
		});

		dc.service.GetServiceDeploymentConfig(service, deployment, {
			failure: function(message){
				alert("Failed to get service deployment details: " + message);
			},

			success: function(response){
				var config = response.properties;
				var data = new Array();
				if(config != undefined){
					for(i = 0; i < config.length; i++){
						data[i] = [config[i].name, config[i].value];
					}			
				}
				
				centerPane.prepend(
						$.fn.create("div")
						.tabularexpandopanel(
							{
							  title: "Details", 
							  data:  data
			                }
						)
					);
			},
		});
		
		centerPane.append(
			$.fn.create("div")
			.tabularexpandopanel(
				{title: "Configuration", 
				 data: convertUserConfigsToArray(deployment.userConfigs)
                 }
			)
		);
		
		$(detailPaneId).replaceContents(serviceDetailPane, detailLayoutOptions);

	};
	
	var resolveUserConfigValue =  function(uc, delim, showRaw){
		showRaw = (showRaw == undefined ? false : showRaw);
		delim   = (delim == undefined ? ", " : delim);
		if(uc.inputType == "TEXT"){
			return uc.displayValue;
		}else {
			var value = "";
			if(uc.options != undefined){
    			var options = uc.options;
    			var count =	 0;
    			for(var i = 0; i < options.length; i++){
    				var option = options[i];
    				if(option.selected.toString() == "true"){
    					if(count > 0) value+=delim;
	    				value += (showRaw ? option.value : option.displayName);
	    				count++;
    				}
    			}
    			return value;
			}else{
				var value = "no value";
			}
		}
	};
	
	var convertUserConfigsToArray = function(userConfigs){
		var a = new Array();
		var defs = new Array();
		var uc;
		for(u in userConfigs){
			uc = userConfigs[u];
			if(uc.exclusion == "EXCLUSION_DEFINITION"){
				defs.push(uc);
			}
		}
		
		for(u in userConfigs){
			uc = userConfigs[u];
			var row = [uc.displayName, resolveUserConfigValue(uc)];
			var addRow = false;
			if(uc.exclusion != undefined && uc.exclusion != "EXCLUSION_DEFINITION"){
				var exclusions = uc.exclusion.split("|");
				var k,m,n,defVals;
				for(k in defs){
					defVals = resolveUserConfigValue(defs[k], "|", true);
					defVals = defVals.split("|");
					for(m in exclusions){
						for(n in defVals){
							if(exclusions[m] == defVals[n]){
								addRow = true;
								break;
							}
						}
						if(addRow) break;
					}
					if(addRow) break;
				}
			}else{
				addRow = true;
			}

			if(addRow){
				a.push(row);
			}
		}
		return a;
	};
	
	////////////////////////////////////////////////////////
	///util functions for handling service deployment element ids
	var deriveDeploymentId = function(service, deployment){
		return service.id + "-" + deployment.id;
	};

	var extractServiceId = function(id){
		return id.split("-")[0];
	};

	var extractDeploymentId = function(id){
		return id.split("-")[1];
	};


	var loadDeployedServiceList = function(services){
		//alert("services count = " + services.length);
		var servicesList = $(servicesListId);
		var panel = $("#deployed-services");
		var table = $("#deployed-services-table");
		
		table.hide();
		$(".dc-message", panel).remove();
		
		servicesList.selectablelist({selectable: false});
		servicesList.selectablelist("clear");
		
		if(services == null || services == undefined || services.length == 0){
			panel.append($.fn.create("span").addClass("dc-message").html("No services are currently deployed."));
			return;
		}
		
		
		for(i in services){
			var service = services[i];
			for(d in service.deployments){
				var deployment = service.deployments[d];
				var item =  $.fn.create(("tr"))
								.attr("id", deriveDeploymentId(service,deployment))
								.addClass("dc-item")
								.addClass(resolveServiceCssClass(service))
								.append($.fn.create("td").addClass("icon").append($.fn.create("div")))
								.append($.fn.create("td").html(service.displayName + " - " + service.serviceVersion))
								.append($.fn.create("td").html(deployment.hostname))
								.append($.fn.create("td").html(deployment.status[0]));
								
				servicesList.selectablelist('addItem',item,{service:service, deployment:deployment});	   
			}
		}


		table.show();
		//bind for current item change listener
		servicesList.bind("currentItemChanged", function(evt,state){
			var currentItem = state.currentItem;
			var service = null;
			var deployment = null;
			if(currentItem != null){
				var data = currentItem.data;
				if(data != null || data != undefined){
					service = data.service;
					deployment = data.deployment;
				}
			}
			loadDeploymentDetail(service,deployment);
		});


	};
	
	
	var refreshDeployedServices = function(){
		
		dc.service.GetDeployedServices({
			begin: function(){
				dc.busy("Retrieving services...");
			},
			success: function(data){
				dc.done();
				loadDeployedServiceList(data.services);
				var id = $(detailPaneId + " #deployment-id").val();
				if(id != null && id != undefined){
					$(servicesListId).selectablelist("setCurrentItemById", id, true);
				}else{
					$(servicesListId).selectablelist("setFirstItemAsCurrent");
				}
			},
			failure: function(text){
				dc.done();
				alert("get available services failed: " + text);
			},
		});
	};

	//dialogs
	var dialogLayout; 
	
	var deployServiceConfig = $("#deploy-service-config").serviceconfig({});

	$('#available-services-dialog').dialog({
		autoOpen: false,
		show: 'fade',
		hide: 'fade',
		resizable: false,
		height: 500,
		closeOnEscape:true,
		modal: true,
		width:700,
		
		buttons: {
			Cancel: function(){
				$(this).dialog("close");
			},
			Next: function(){
				var currentItem = $("#available-services-list").selectablelist("currentItem");
				if(currentItem == null || currentItem == undefined){
					alert("You must select a service.");
					return;
				}

				$(this).dialog("close");
				
				var service = currentItem.data;
				deployServiceConfig.serviceconfig("load", service);
				$("#configure-service-dialog").dialog("open");
			}
		
		},
		close: function() {
	
		},
		
		
		
		open: function(e){	
			var that = this;
			
			var showMessage = function(text){
				$(".dc-message", that).html(text);
			};
			
			if(!dialogLayout){
				dialogLayout = $(this).layout({
					resizeWithWindow:	false	// resizes with the dialog, not the window
					,  spacing_open:  0
					,  spacing_closed: 0
					,	west__spacing_open:		6
					,	west__spacing_closed:	6
					,	west__size:			300
					,   north__resizable: 			false
					,	north__slidable: 			false
					,   west__slidable: 		true
					,   west__resizable:        true
					
				});
			}
			
			$("#available-services-dialog").listdetailviewer(
				{
						selectableListId: "available-services-list"
					,	detailId:  "service-detail"
					,   detailPreparer: function(data){
							return $.fn.create("div")
										.append($.fn.create("h2").html(data.displayName))
										.append($.fn.create("p").html(data.description));
						}
				}
			);
			
			
			dc.service.GetAvailableServices({ 
				begin: function(){
					$("#available-services-dialog .dc-message").html("Loading...");
				},
				success: function(data){
					showMessage("");
					var services = data.services;
					if(services == undefined){
						showMessage("There are no available services.");
						return;
					}
					for(i in services){
						var service = services[i];
						
						$("#available-services-list")
							.selectablelist("addItem", $.fn.create("tr")
												.attr("id", service.id)
												.addClass(dc.getServiceTypeImageClass(service.serviceName))
												.append($.fn.create("td").addClass("icon").append($.fn.create("div")))
												.append($.fn.create("td").html(service.displayName)), service);

					}
				},
				failure: function(text, xhr){
					showMessage("Unable to load services: " + xhr.responseText);
				},
			});
			
			

		},
		
	});

	
	
	$(".deploy-service-button").click(function(){
		$("#available-services-dialog").dialog("open");
	});

	var serviceConfig = $("#reconfigure-service-config").serviceconfig({});

	$('#reconfigure-service-dialog').dialog({
		autoOpen: false,
		show: 'fade',
		hide: 'fade',
		resizable: false,
		height: 600,
		width:800,
		closeOnEscape:true,
		modal: true,
		buttons: {
			"Redeploy": function(){
		
				var data = serviceConfig.serviceconfig("data");
				$("#reconfigure-service-dialog").dialog("close");
				var formValues = serviceConfig.serviceconfig("serializedFormValues");

				dc.busy("Reploying " + data.service.displayName, {modal:true});
				dc.ajax({
					url: "/duradmin/services/service?method=reconfigure",
					data: formValues, 
					type: "POST",
					success: function(data){
						dc.done();
						refreshDeployedServices();
					},
				    failure: function(textStatus){
						dc.done();
				    	alert("failed: " + textStatus);
				    },
				});

			},
			
			"Cancel": function(){
				$("#reconfigure-service-dialog").dialog("close");
			}
		},

		close: function() {
		
		},
		open: function(e){
			var currentItem = $(servicesListId).selectablelist("currentItem");
			
			
			if(currentItem == null || currentItem == undefined){
				$(this).dialog("close");
				alert("You must select a service deployment.");
				return;
			}

			var data = currentItem.data;

			serviceConfig.serviceconfig("load", data.service, data.deployment);
			
		},
	});
	


	
	$('#configure-service-dialog').dialog({
		autoOpen: false,
		show: 'fade',
		hide: 'fade',
		resizable: false,
		height: 600,
		width:800,
		closeOnEscape:true,
		modal: true,
		close: function() {
		
		},
		
		buttons: {
			"< Back": function(){
				$("#configure-service-dialog").dialog("close");
				$("#available-services-dialog").dialog("open");
			},
			
			"Deploy": function(){
				
				var service = deployServiceConfig.serviceconfig("data").service;
				$("#configure-service-dialog").dialog("close");
				var data = deployServiceConfig.serviceconfig("serializedFormValues");
				dc.busy("Deploying " + service.displayName, {modal:true});
				dc.ajax({
					url: "/duradmin/services/service?method=deploy",
					data: data, 
					type: "POST",
					success: function(data){
						dc.done();
						loadDeploymentDetail(data.serviceInfo, data.deployment);
						refreshDeployedServices();
					},
				});

			},
			
			"Cancel": function(){
				$("#configure-service-dialog").dialog("close");
			}
		},
		
		open: function(e){
		},
	});
	
	setTimeout(function(){
		refreshDeployedServices();
	}, 1000);

});