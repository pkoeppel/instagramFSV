let saveTemp = [];
let currentMenMatchDetails = null;

async function getAllYouthMatches() {
    document.getElementById('created').textContent = 'Erledigt: ';
    const matchList = document.getElementById('matchesList');
    matchList.replaceChildren();
    try {
        const response = await fetch(window.location.origin + '/getAllYouthMatches');
        if (!response.ok) throw new Error('HTTP ' + response.status);
        const games = await response.json();
        const fragment = document.createDocumentFragment();
        games.forEach(game => {
            const matchData = game.team + "; " + game.date + "; " + game.matchType + "; " + game.homeTeam.clubName + " VS " + game.awayTeam.clubName;
            const match = document.createElement('li');
            const controls = document.createElement('span');
            const check = document.createElement('input');
            check.type = 'checkbox';
            check.name = game.date + game.team;
            check.value = matchData;
            check.className = 'checkboxes';
            check.addEventListener('change', () => {
                controls.replaceChildren();
                if (!check.checked) return;
                const matchType = game.matchType.toLowerCase();
                const isFestival = matchType.includes('kinder');
                const isLeague = matchType.includes('liga') || matchType.includes('klasse');
                let resultInput = null;
                let homeStatsInput = null;
                let awayStatsInput = null;
                if (!isFestival) {
                    resultInput = document.createElement('input');
                    resultInput.type = 'text';
                    resultInput.placeholder = 'Ergebnis';
                    controls.append(resultInput);
                    if (isLeague) {
                        homeStatsInput = document.createElement('textarea');
                        homeStatsInput.rows = 2;
                        homeStatsInput.value = 'Platz  ( / :) \nTrend: --';
                        awayStatsInput = document.createElement('textarea');
                        awayStatsInput.rows = 2;
                        awayStatsInput.value = 'Platz  ( / :) \nTrend: --';
                        controls.append(homeStatsInput, awayStatsInput);
                    }
                }
                const saveButton = document.createElement('input');
                saveButton.type = 'button';
                saveButton.value = 'Speichern';
                saveButton.addEventListener('click', () => {
                    const value = {
                        id: game,
                        result: resultInput?.value || null,
                        homeStats: homeStatsInput?.value || null,
                        awayStats: awayStatsInput?.value || null,
                        text: ''
                    };
                    if (!isFestival) ensureTeamReport(game.team);
                    upsertSavedResult(value);
                    showSaveTemp(game);
                });
                const cancelButton = document.createElement('input');
                cancelButton.type = 'button';
                cancelButton.value = 'Absagen';
                cancelButton.addEventListener('click', () => {
                    upsertSavedResult({id: game, result: null, homeStats: null, awayStats: null, text: 'Abgesagt'});
                    showSaveTemp(game);
                });
                const deleteButton = document.createElement('input');
                deleteButton.type = 'button';
                deleteButton.value = 'Löschen';
                deleteButton.addEventListener('click', () => deleteMatchEntry('youth', JSON.stringify(game)));
                controls.append(saveButton, cancelButton, deleteButton);
            });
            match.append(check, document.createTextNode(matchData), controls);
            fragment.append(match);
        });
        matchList.append(fragment);
    } catch (error) {
        alert("Es ist ein Fehler beim Laden aufgetreten: " + error);
        console.error('Error: ', error);
    }
}

function upsertSavedResult(value) {
    const index = saveTemp.findIndex(entry => entry.id === value.id);
    if (index >= 0) saveTemp.splice(index, 1);
    saveTemp.push(value);
}

function ensureTeamReport(team) {
    const report = document.getElementById('reportboxes');
    const existing = Array.from(report.querySelectorAll('textarea')).find(textarea => textarea.dataset.team === team);
    if (existing) return existing;
    const textArea = document.createElement('textarea');
    textArea.dataset.team = team;
    textArea.rows = 4;
    const label = document.createElement('label');
    label.textContent = team + ':';
    report.append(label, textArea);
    return textArea;
}

async function getMenMatches(loadDetails = true) {
    const select = document.getElementById("matches");
    select.replaceChildren();
    select.onchange = loadDetails ? loadMenMatchDetails : null;
    try {
        const response = await fetch(window.location.origin + '/getAllMenMatches');
        if (!response.ok) throw new Error('HTTP ' + response.status);
        const matches = await response.json();
        const fragment = document.createDocumentFragment();
        matches.forEach(match => {
            const option = document.createElement("option");
            option.text = match.matchDate + ", " + match.homeClub.clubName + " VS " + match.awayClub.clubName + ", " + match.competition;
            option.value = JSON.stringify(match);
            fragment.append(option);
        });
        select.append(fragment);
        if (select.options.length > 0 && loadDetails) loadMenMatchDetails();
    } catch (error) {
        alert("Es ist ein Fehler beim Laden aufgetreten: " + error);
        console.error('Error: ', error);
    }
}

function loadMenMatchDetails() {
    const select = document.getElementById('matches');
    const details = document.getElementById('matchDetails');
    currentMenMatchDetails = null;
    details.textContent = 'Ergebnis und Torschützen werden geladen ...';
    setCharCount();
    const selectedMatch = select.value;
    const params = new URLSearchParams({match: selectedMatch});
    fetch(window.location.origin + '/getMenMatchDetails?' + params.toString())
        .then(response => {
            if (!response.ok) {
                throw new Error('HTTP ' + response.status);
            }
            return response.json();
        })
        .then(data => {
            if (select.value === selectedMatch) {
                currentMenMatchDetails = data;
                details.textContent = data.matchLine.trim() + '\n\n' + data.scorers;
                setCharCount();
            }
        })
        .catch(error => {
            if (select.value === selectedMatch) {
                details.textContent = 'Ergebnis und Torschützen konnten nicht geladen werden.';
                console.error('Error: ', error);
                setCharCount();
            }
        });
}

function deleteMatchEntry(team, game) {
    if (confirm("Soll das Spiel wirklich gelöscht werden?")) {
        let formData = new FormData();
        formData.append("game", game);
        formData.append("team", team);
        fetch(window.location.origin + '/deleteMatchEntry', {
            method: 'POST',
            mode: 'cors',
            cache: 'no-cache',
            credentials: 'same-origin',
            redirect: 'follow',
            referrerPolicy: 'no-referrer',
            body: formData,
        })
            .then(response => {
                if (response.status === 200) {
                    alert("Erfolgreich entfernt!");
                    window.location.reload();
                } else
                    alert("Fehler beim entfernen. Probiere es später erneut.");
            })
            .catch((error) => {
                alert("Es ist ein Fehler beim Löschen aufgetreten: " + error);
                console.error('Error: ', error);
            });
    }
}

function postYouthResults() {
    let caption = document.getElementById("headline").value + " 🔴🟢🟡" + "\n\n";
    let resDev = document.getElementById("reportboxes");
    let elem = resDev.querySelectorAll("textarea");
    for (let i = 0; i < elem.length; i++) {
        caption += elem[i].dataset.team + ":\n" + elem[i].value + "\n\n";
    }
    fetch(window.location.origin + '/postYouthResults', {
        method: 'POST',
        mode: 'cors',
        cache: 'no-cache',
        credentials: 'same-origin',
        headers: {'Content-Type': 'application/json',},
        redirect: 'follow',
        referrerPolicy: 'no-referrer',
        body: JSON.stringify(saveTemp)
    })
        .then(response => response.json())
        .then((data) => {
            console.log(data);
            for (let [key, value] of Object.entries(data)) {
                for (let i = 1; i <= value; i++) {
                    window.open(window.location.origin + '/download/youth/' + key + '/Result' + i + '.jpeg');
                }
            }
            let repDiv = document.getElementById('showReport');
            repDiv.innerText = caption;
        })
        .catch((error) => {
            alert("Es ist ein Fehler beim Erstellen aufgetreten: " + error);
            console.error('Error: ', error);
        });
}