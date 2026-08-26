async function getStoredMenMatches() {
    const select = document.getElementById('matches');
    select.replaceChildren();
    try {
        const response = await fetch(window.location.origin + '/getAllMenMatches');
        if (!response.ok) throw new Error('HTTP ' + response.status);
        const matches = await response.json();
        const fragment = document.createDocumentFragment();
        matches.forEach(match => {
            const option = document.createElement('option');
            option.text = match.matchDate + ', ' + match.homeClub.clubName + ' VS ' + match.awayClub.clubName + ', ' + match.competition;
            option.value = JSON.stringify(match);
            fragment.append(option);
        });
        select.append(fragment);
    } catch (error) {
        alert('Es ist ein Fehler beim Laden der gespeicherten Spiele aufgetreten: ' + error);
        console.error('Error: ', error);
    }
}
