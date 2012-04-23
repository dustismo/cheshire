Cheshire = {};
Cheshire.typeOf = function(v) {
  if (typeof(v) == "object") {
    if (v === null) return "null";
    if (v.constructor == (new Array).constructor) return "array";
    if (v.constructor == (new Date).constructor) return "date";
    if (v.constructor == (new RegExp).constructor) return "regex";
    return "object";
  }
  return typeof(v);
};

/**
 * gets the value from the passed in object
 * 
 * advantage is that it will accept dot operator
 * 
 * so object[key1.key2] will work
 * 
 */
Cheshire.dictGet = function(object, key, defaultVal) {
	try {
		var val = object;
		$.each(key.split('.'), function() {
			val = val[this];
		});
	} catch (e) {
		return defaultVal;
	}
	if (val == null)
		return defaultVal;
	return val;
};

Cheshire.formatJSON = function(oData, sIndent) {
    if (arguments.length < 2) {
        var sIndent = "";
    }
    var sIndentStyle = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
    var sDataType = Cheshire.typeOf(oData);

    // open object
    if (sDataType == "array") {
        if (oData.length == 0) {
            return "[]";
        }
        var sHTML = "[";
    } else {
        var iCount = 0;
        $.each(oData, function() {
            iCount++;
            return;
        });
        if (iCount == 0) { // object is empty
            return "{}";
        }
        var sHTML = "{";
    }

    // loop through items
    var iCount = 0;
    $.each(oData, function(sKey, vValue) {
        if (iCount > 0) {
            sHTML += ",";
        }
        if (sDataType == "array") {
            sHTML += ("<br />" + sIndent + sIndentStyle);
        } else {
            sHTML += ("<br />" + sIndent + sIndentStyle + "\"<span class=\"json-key\">" + sKey + "\"</span>" + ": ");
        }

        // display relevant data type
        switch (Cheshire.typeOf(vValue)) {
            case "array":
            case "object":
                sHTML += Cheshire.formatJSON(vValue, (sIndent + sIndentStyle));
                break;
            case "boolean":
            case "number":
                sHTML += vValue.toString();
                break;
            case "null":
                sHTML += "null";
                break;
            case "string":
                sHTML += ("\"" + vValue + "\"");
                break;
            default:
                sHTML += ("TYPEOF: " + typeof(vValue));
        }

        // loop
        iCount++;
    });

    // close object
    if (sDataType == "array") {
        sHTML += ("<br />" + sIndent + "]");
    } else {
        sHTML += ("<br />" + sIndent + "}");
    }

    // return
    return sHTML;
}


/**
 * Usage: 
 * 
 * CheshireApi.request({});
 * 
 * see options for request below.
 * 
 * TODO: this is ajax, need a websockets version.
 * 
 */
CheshireApi = (function() {
	//private
	
	
	//public 
	return {
		/**
		 * Options:
		 * 
		 * api => the endpoint ex: /v4/system/ping
		 * method => GET, POST, DELETE, PUT
		 * params => params to pass to the api.
		 * form => form element to get params from, will override 
		 * on_success => the result of the api call: function(request, result)
		 * on_error => an error was returned function(request, code, message)
		 * on_complete => the request is finished, really only applicable for stream requests. function(request)
		 */
		request : function(config) {
			var options = $.extend({
				params: {},
				method: 'GET',
				on_success: function(request, result) {
					console.log(request);
					console.log(result);
				},
				on_error: function(request, code, message) {
					console.log("ERROR: " + code + " -- " + message);
				},
				on_complete: function(request) {
					//do nothing.
				}
			},config);
						
			if((options.dataType == undefined)||(options.dataType == '')){
				options.dataType = 'json';
			} 
			
			if (options.form) {
				options.params = $.extend(options.params,Cheshire.serialize(options.form));
			}
			
			//allow params to be a request string.
			if (options.method == 'GET' && Cheshire.typeOf(options.params) == 'string'){
				options.api = options.api + options.params;
			} 
			
			$.ajax({
				  type: options.method,
				  url: options.api,
				  dataType: options.dataType,
				  data: Cheshire.urlEncodeParams(options.params),
				  success: function(obj){
					 options.on_success(options, obj);
					 if (options.on_complete) {
						 options.on_complete(options);
					 }
				  },
				  error: function(XMLHttpRequest, textStatus, errorThrown){
					  options.on_error(options, XMLHttpRequest.status, errorThrown);
					  if (errorThrown)
						  console.log("Error " + errorThrown);
					  if (options.on_complete) {
						  options.on_complete(options);
					  }
				  }
			});
		}
	}
}())


Cheshire.urlEncode = function(str) {
	return encodeURIComponent(str);
};

Cheshire.urlEncodeParams = function(params) {
	
	//urlEncode the params..
	if (!params) {
		return params;
	}

	if (Cheshire.typeOf(params) == 'string') 
		return params

	var pms = {};
	for (param in params) {
		pms[Cheshire.urlEncode(param)] = Cheshire.urlEncode(params[param]);
	}
	return pms;
};
