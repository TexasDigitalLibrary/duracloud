/**
 * 
 * @author Daniel Bernstein
 */

/**
 * Durastore API 
 */

var dc; 
(function(){
	if(dc == undefined){
		dc ={};
	}
	
	dc.store = {};
	
	/**
	 * @param String spaceId  space id 
	 * @param Object callback The callback must implement success and failure methods.
	 * @option Function success(spaces) spaces is an array of spaces objects
	 * @option Function failure(info) 
	 * @param Object options
	 * @option String marker - the last content item id of the previous page
	 * @option String prefix - a filters the results to show only those matching the prefix
	 */

	 dc.store.GetSpace = function(storeProviderId,spaceId,callback,options){
		if(options == undefined){
			options = {};
		}
		
		var marker = null;
		if(options.marker != undefined){
			marker = options.marker;
		}

		var prefix = null;
		if(options.prefix != undefined){
			prefix = options.prefix;
		}

		
		dc.ajax({
			url: "/duradmin/spaces/space", 
			data: "storeId="+storeProviderId+"&spaceId="+escape(spaceId)+"&prefix="+escape(prefix==null?'':prefix)+"&marker="+escape(marker==null?'':marker),
			cache: false,
			context: document.body,
			success: function(data){
				callback.success(data.space);
			},
		}, callback);		
	};
	
	/**
	 * @param Object space
	 * @param Object callback The callback must implement success and failure methods. options begin method is supported.
	 */
	dc.store.DeleteSpace = function(space, callback){
		dc.ajax({
			url: "/duradmin/spaces/space", 
			data: "action=delete&storeId="+space.storeId+"&spaceId="+escape(space.spaceId),
			type: "POST",
			success: callback.success,
		    failure: callback.failure,
		}, callback);
	};

	/**
	 * @param String space  space
	 * @param String access  access 
	 * @param Object callback The callback must implement success and failure methods. options begin method is supported.
	 */
	dc.store.AddSpace = function(space, callback){
		dc.ajax(
			{
				url: "/duradmin/spaces/space", 
				data: "storeId="+space.storeId+"&spaceId="+escape(space.spaceId)+"&access="+space.access,
				type: "POST",
				success: function(data){
					callback.success(data.space)
				},
			    failure:callback.failure,
			},callback);
	};

	/**
	 * Returns a list of spaces
	 * @param Number | String storeProviderId The id of the store provider
	 * @param Object callback
	 * @option Function success(spaces) a handler for an array of spaces
	 * @option Function failure(info) a handler that returns failure info 
	 */
	dc.store.GetSpaces = function(storeProviderId, callback){
		dc.ajax({ 
				url: "/duradmin/spaces", 
				data: "storeId="+storeProviderId+"&f=json",
				cache: false,
				success: function(data){
					callback.success(data.spaces)
				},
				failure:callback.failure,
		},callback);
		
	};
	
	
	/**
	 * returns contentItem details
	 */
	dc.store.GetContentItem = function(storeProviderId, spaceId, contentItemId, callback){
		dc.ajax({
				url: "/duradmin/spaces/content",
				data: "storeId="+storeProviderId+"&spaceId="+escape(spaceId)+"&contentId="+escape(contentItemId),
				cache: false,
				success: function(data){
					callback.success(data.contentItem);
			    },
			    failure: callback.failure,
		},callback);

	};
	
	/**
	 * @param Object contentItem
	 * @param Object callback The callback must implement success and failure methods. options begin method is supported.
	 */
	dc.store.DeleteContentItem = function(contentItem, callback){
		dc.ajax({
			url: "/duradmin/spaces/content", 
			data: "action=delete&storeId="+contentItem.storeId+"&spaceId="+escape(contentItem.spaceId)+"&contentId="+escape(contentItem.contentId),
			type: "POST",
			success: callback.success,
		    failure: callback.failure,
		},callback);
	};


	/**
	 * @param Object serialized form data
	 * @param Object callback The callback must implement success and failure methods. options begin method is supported.
	 */
	dc.store.UpdateContentItemMimetype = function(data, callback){
		dc.ajax({
			url: "/duradmin/spaces/content", 
			data: data + "&action=put&method=changeMimetype",
			type: "POST",
			success: function(data,xhr){
				if(data.contentItem != undefined){
					callback.success(data.contentItem);
				}else{
					this.failure(data,xhr);
				}
			},
		    failure: callback.failure,
		},callback);
	};

	/**
	 * Adds a content item.  
	 * @param Object/String a reference to a form dom node or the form's id
	 * @param Object future an object implementing a success and failure method.
	 */
	dc.store.AddContentItem = function(form, future){
		future.begin();
		$(form).ajaxSubmit({
			iframe: true,
			dataType: 'json',
			success: function(data){
				dc.checkSession(data);
				future.success(data);
		    },
		    error: function(xhr, status, errorThrown){
		    	future.failure(status,xhr);
		    },
		});
	};
	
	/**
	 * checks if the content item already exists
	 * @param spaceId
	 * @param contentId
	 * @param storeId 
	 * 
	 */
	dc.store.CheckIfContentItemExists = function(contentItem, callback){
		dc.store.GetContentItem(contentItem.storeId,contentItem.spaceId,contentItem.contentId,{
			begin: function(){
			},
			
			failure: function(text){
				callback.success(false);
			},

			success: function(contentItem){
				callback.success(contentItem != undefined);
			},
		});
	};
	
	/**
	 * 
	 */
	dc.store.formatJ2kViewerURL = function(/*string*/j2kViewerBaseURL, /*object*/contentItem){
		return j2kViewerBaseURL + "/viewer.html?rft_id=" + escape(contentItem.durastoreURL);
	};	

	
	/**
	 * 
	 */
	dc.store.formatThumbnail = function(/*object*/contentItem, /*int*/ size, /*optional - string*/j2kViewerBaseURL ){
    	return j2kViewerBaseURL+
    			"/resolver?url_ver=Z39.88-2004&rft_id="+escape(contentItem.durastoreURL)+"&" + 
                "svc_id=info:lanl-repo/svc/getRegion&svc_val_fmt=info:ofi/fmt:kev:mtx:jpeg2000&" +
                "svc.format=image/png&svc.level="+size+"&svc.rotate=0&svc.region=0,0,500,500";
    };


	var GENERIC_THUMBNAIL_PREFIXES = ["image", "video", "text", "pdf"];
	
	/**
	 * 
	 */
    dc.store.formatGenericThumbnail = function(/*object*/contentItem){
        var mimetype = contentItem.metadata.mimetype;
    	var gtf,i;
    	for(i in GENERIC_THUMBNAIL_PREFIXES){
    		gtf = GENERIC_THUMBNAIL_PREFIXES[i];
    		if(mimetype.indexOf(gtf) == 0){
    			return "/duradmin/images/generic-thumb-" + gtf + ".png";
    		}
    	}
    	return "/duradmin/images/generic-thumb-other.png";
    };
    
    /**
     * 
     */
	dc.store.formatDownloadURL = function(/*object*/contentItem, /*boolean*/ asAttachment){
		if(asAttachment == undefined) asAttachment = true;
		return "/duradmin/download/contentItem?spaceId=" +  contentItem.spaceId + 
			   "&contentId=" + contentItem.contentId + "&storeID=" + contentItem.storeId + 
			   "&attachment=" + asAttachment;
	};	

})();

