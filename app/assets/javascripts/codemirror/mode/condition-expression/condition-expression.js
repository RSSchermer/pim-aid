(function(mod) {
    mod(CodeMirror);
})(function(CodeMirror) {

"use strict";

CodeMirror.defineMode("condition-expression", function(config, parserConfig) {
    function wordRE(words) {
        return new RegExp("^(?:" + words.join("|") + ")$", "i");
    }

    var validCEVariables = window.validCEVariables || [];

    var keywords = wordRE(["and","false","not","or","true"]);

    return {
        startState: function(basecol) {
            return {basecol: basecol || 0, indentDepth: 0, cur: null};
        },

        token: function(stream, state) {
            if (stream.eatSpace()) return null;

            var ch = stream.next();

            if (/[\w_]/.test(ch)) {
                stream.eatWhile(/[\w\\\-_.]/);

                var word = stream.current();

                if (keywords.test(word)) return "keyword";
            } else if (ch === "[") {
                stream.eatWhile(/[^\]]/);

                var word = stream.current();

                stream.next();

                if (validCEVariables.indexOf(word.substring(1)) >= 0) {
                    return "valid-term";
                } else {
                    return "invalid-term";
                }
            }

            return null;
        }
    };
});

});
