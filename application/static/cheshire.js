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
    var sDataType = Trendrr.typeOf(oData);

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
        switch (Trendrr.typeOf(vValue)) {
            case "array":
            case "object":
                sHTML += Trendrr.formatJSON(vValue, (sIndent + sIndentStyle));
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
