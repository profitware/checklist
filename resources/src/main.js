var initApp = function ($, page_name, content) {
  $(function () {
    var alt_pressed = false;
    $('body').keydown(function (event) {
      if (alt_pressed) {
        switch (event.which) {
        case 84:
          window.location.href = '.';
          break;
        case 67:
          window.location.href = 'cards';
          break;
        case 83:
          window.location.href = 'schedule';
          break;
        case 76:
          window.location.href = 'logout';
        }
        event.preventDefault();
      }
      alt_pressed = (event.which == 18);
    }).keyup(function (event) {
      alt_pressed = false;
    });

    if (page_name === 'cards' || page_name === 'schedule') {
      var contentCodeMirror = CodeMirror(document.getElementById('codemirror'), {
        value: content,
        theme: 'mdn-like',
        lineNumbers: true,
        autofocus: true,
        styleActiveLine: true,
        matchBrackets: true
      });
      contentCodeMirror.execCommand('goDocEnd');
      parinferCodeMirror.init(contentCodeMirror);
      $('button.close').click(function () { $(this).closest('.alert').hide(); });
      $('.action-button').click(function () {
        var payload = {};
        payload[page_name + '-code'] = contentCodeMirror.getValue();
        $('.alert-danger').hide();
        $('.alert-success').hide();
        $.ajax({
          type: 'POST',
          url: page_name + '/ajax',
          data: JSON.stringify(payload),
          contentType: 'application/json'
        }).done(function (data) {
          if (data[page_name + '-code']) {
            contentCodeMirror.getValue(data[page_name + '-code']);
            $('.alert-success').show();
          } else if (data['error']) {
            $('.alert-danger').show();
          }
        });
      });
      contentCodeMirror.setOption('extraKeys', {
        'Ctrl-Enter': function(cm) {
          $('.action-button').click();
        }
      });
    } else {
      var checkbox_selector = 'input[type=\checkbox\]';

      var got_data_callback_factory = function ($card_pf) {
        var update_checkboxes = function($card_pf, data) {
          var card_id = $card_pf.attr('id'),
              cards = data.cards,
              checked_count = 0;
          if (cards) {
            $(cards).each(function () {
              if (this['card-id'] == card_id) {
                $(this['card-checkboxes']).each(function () {
                  var checked_this = this['checkbox-checked'];
                  if (checked_this) {
                    checked_count += 1;
                  }
                  $card_pf.find(checkbox_selector + '#' + this['checkbox-id'])
                    .prop('checked', checked_this);
                });
              }
            });
          }
          if (checked_count == $card_pf.find(checkbox_selector).length) {
            $card_pf.addClass('card-disabled');
          } else {
            $card_pf.removeClass('card-disabled');
          }
        };


        return function (data) {
          if (data.cards) {
            if ($card_pf) {
              update_checkboxes($card_pf, data);
            } else {
              $('div.cards-content').find('.card-pf').each(function () {
                update_checkboxes($(this), data);
              });
            }
          }
        }
      };

      $(checkbox_selector).click(function () {
        var $card_pf = $(this).closest('.card-pf'),
            card_id = $card_pf.attr('id'),
            payload = {
              'card-id': card_id,
              'checked': $card_pf.find(checkbox_selector + ':checked').map(function (i, el) {
                return $(this).attr('name');
              }).get()
            };
        $.ajax({
          type: 'POST',
          url: page_name + '/ajax',
          data: JSON.stringify(payload),
          contentType: 'application/json'
        }).done(got_data_callback_factory($card_pf));
      });

      setInterval(function () {
        $.ajax({
          type: 'GET',
          url: page_name + '/ajax',
          contentType: 'application/json'
        }).done(got_data_callback_factory());
      }, 30 * 1000);
    }
  });
};
