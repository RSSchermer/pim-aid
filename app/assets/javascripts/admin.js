$(document).ready(function () {
  var removeSuggestion = function () {
    $(this).parent('.suggestion-fieldset').remove();
  };

  var suggestionCount = $('.suggestion-fieldset').length;

  $('.add-suggestion-btn').click(function () {
    var index = suggestionCount;

    var suggestionFieldset = $(
      '<fieldset class = "suggestion-fieldset">'+
        '<div class="clearfix"  id="suggestions_'+ index +'_text_field">'+
          '<label for="suggestions_'+ index +'_text">suggestions.'+ index +'.text</label>'+
          '<div class="input">'+
            '<textarea id="suggestions_'+ index +'_text" name="suggestions['+ index +'].text" ></textarea>'+
          '</div>'+
        '</div>'+
        '<div class="clearfix" id="suggestions_'+ index +'_explanatoryNote_field">'+
          '<label for="suggestions_'+ index +'_explanatoryNote">suggestions.'+ index +'.explanatoryNote</label>'+
          '<div class="input">'+
            '<textarea id="suggestions_'+ index +'_explanatoryNote" name="suggestions['+ index +'].explanatoryNote" ></textarea>'+
          '</div>'+
        '</div>'+
        '<a class="remove-suggestion-btn">Remove suggestion</a>'+
      '</fieldset>'
    );

    $('.suggestion-fields').append(suggestionFieldset);

    suggestionFieldset.find('.remove-suggestion-btn').click(removeSuggestion);

    suggestionCount++;
  });

  $('.suggestion-fields').find('.remove-suggestion-btn').click(removeSuggestion);
});