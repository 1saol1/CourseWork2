import random

def eng_word_random():
    syllables = [
        'ab', 'ac', 'ad', 'af', 'ag', 'al',
        'am', 'an', 'ap', 'ar', 'as', 'at',
        'ba', 'be', 'bi', 'bo', 'bu', 'by',
        'ca', 'ce', 'ci', 'co', 'cu', 'cy',
        'da', 'de', 'di', 'do', 'du', 'dy',
        'ed', 'ef', 'eg', 'el', 'em', 'en',
        'ep', 'er', 'es', 'et', 'ev', 'ex',
        'fa', 'fe', 'fi', 'fo', 'fu', 'fy',
        'ga', 'ge', 'gi', 'go', 'gu', 'gy',
        'ha', 'he', 'hi', 'hu', 'hy',
        'ic', 'id', 'ig', 'il', 'im', 'ip',
        'ir', 'is', 'it',
        'ja', 'je', 'ji', 'jo', 'ju',
        'ka', 'ke', 'ki', 'ko', 'ku',
        'la', 'le', 'li', 'lo', 'lu', 'ly',
        'ma', 'me', 'mi', 'mo', 'mu', 'my',
        'na', 'ne', 'ni', 'no', 'nu', 'ny',
        'oc', 'od', 'of', 'og', 'ol', 'om',
        'on', 'op', 'or', 'os', 'ot',
        'pa', 'pe', 'pi', 'po', 'pu', 'py',
        'qu',
        'ra', 're', 'ri', 'ro', 'ru', 'ry',
        'sa', 'se', 'si', 'so', 'su', 'sy',
        'ta', 'te', 'ti', 'to', 'tu', 'ty',
        'ub', 'uc', 'ud', 'uf', 'ug', 'ul',
        'um', 'un', 'up', 'ur', 'us', 'ut',
        'va', 've', 'vi', 'vo', 'vu',
        'wa', 'we', 'wi', 'wo', 'wy',
        'ya', 'ye', 'yo', 'yu',
        'za', 'ze', 'zi', 'zo', 'zu'
    ]

    word_length = random.randint(2, 3)
    word = ''.join(random.choice(syllables) for _ in range(word_length))

    if random.random() > 0.5:
        word = word.capitalize()

    return word

for _ in range(15):
    print(eng_word_random())