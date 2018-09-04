var initApp = function ($, page_name, token, content) {
  var htmlDecode = function (input) {
    var doc = new DOMParser().parseFromString(input, "text/html");
    return doc.documentElement.textContent;
  };
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

    $('button.close').click(function () {
      if (!($(this).hasClass('hide-card'))) {
        $(this).closest('.alert').hide();
      }
    });

    if (page_name === 'cards' || page_name === 'schedule') {
      var contentCodeMirror = CodeMirror(document.getElementById('codemirror'), {
        theme: 'mdn-like',
        lineNumbers: true,
        autofocus: true,
        styleActiveLine: true,
        matchBrackets: true
      });
      contentCodeMirror.setValue(htmlDecode(content));
      contentCodeMirror.execCommand('goDocEnd');
      parinferCodeMirror.init(contentCodeMirror);
      $('.action-button').click(function () {
        var payload = {'token': token};
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
              checked_count = 0,
              card_should_be_hidden = false,
              card_should_be_highlighted = false;
          if (cards) {
            $(cards).each(function () {
              if (this['card-id'] == card_id) {
                card_should_be_hidden = this['card-hidden'];
                card_should_be_highlighted = this['card-highlighted'];
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
          if (card_should_be_hidden) {
            $card_pf.hide();
          } else {
            $card_pf.show();
          }
          if (card_should_be_highlighted) {
            $card_pf.addClass('card-highlighted');
          } else {
            $card_pf.removeClass('card-highlighted');
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
          if ($('div.card-pf:visible').length > 0) {
            $('div#blank').hide();
          } else {
            $('div#blank').show();
          }
        };
      };

      var send_card_info = function () {
        var $card_pf = $(this).closest('.card-pf'),
            card_id = $card_pf.attr('id'),
            payload = {
              'token': token,
              'card-id': card_id,
              'hide': $(this).hasClass('hide-card'),
              'highlight': $card_pf.hasClass('card-highlighted'),
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
      }

      $(checkbox_selector).click(send_card_info);

      $('button.hide-card').click(function () {
        send_card_info.apply($(this));
        $('button.show-card[data-card-id="' + $(this).closest('.card-pf').attr('id') + '"]').show();
        $('p.hidden-message').show();
      });
      $('button.show-card').click(function () {
        var $card_pf = $('div.card-pf#' + $(this).data('card-id'));
        send_card_info.apply($card_pf);
        $(this).hide();
        if ($('button.show-card:visible').length === 0) {
          $('p.hidden-message').hide();
        }
        return false;
      });

      $('.highlight-card').click(function () {
        var $card_pf = $(this).closest('.card-pf');
        $card_pf.toggleClass('card-highlighted');
        send_card_info.apply($card_pf);
        return false;
      });

      setInterval(function () {
        $.ajax({
          type: 'GET',
          url: page_name + '/ajax',
          contentType: 'application/json'
        }).done(got_data_callback_factory());
      }, 10 * 1000);
    }
  });
};