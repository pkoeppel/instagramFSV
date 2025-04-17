let saveTemp = [];

function getAllYouthMatches() {
    let inpRes, tashome, tasaway, textArea, value;
    let homeStats = null;
    let awayStats = null;
    document.getElementById('created').innerHTML = 'Erledigt: ';
    fetch(window.location.origin + '/getAllYouthMatches')
        .then((result) => result.json())
        .then((data) => {
            let matchList = document.getElementById('matchesList');
            data.forEach(element => {
                let game = element.game;
                let home = "Zuhause";
                if (game.homeGame === false) {
                    home = "Auswärts";
                }
                let matchData = game.youth + "; " + game.date + "; " + game.matchType + "; " + game.homeTeam + " VS " + game.awayTeam + "; " + home;
                let match = document.createElement('li');

                let span = document.createElement('span');
                span.id = game.date + game.team;

                let check = document.createElement('input');
                check.type = "checkbox"
                check.name = game.date + game.team;
                check.value = matchData;
                check.className = "checkboxes";
                check.addEventListener('change', function () {
                    if (check.checked) {
                        span.appendChild(document.createElement('br'));
                        if (!game.matchType.toLowerCase().includes("kinder")) {
                            inpRes = document.createElement('input');
                            inpRes.id = "result";
                            inpRes.type = "text";
                            span.appendChild(inpRes);
                            span.appendChild(document.createElement('br'));
                            if (game.matchType.toLowerCase().includes("liga") || game.matchType.toLowerCase().includes("klasse")) {
                                tashome = document.createElement('textarea');
                                tashome.id = "tashome";
                                tashome.rows = 2;
                                tashome.value = "Platz  ( / :) \nTrend: --";
                                tasaway = document.createElement('textarea');
                                tasaway.id = "tasaway";
                                tasaway.rows = 2;
                                tasaway.value = "Platz  ( / :) \nTrend: --";
                                span.appendChild(tashome);
                                span.appendChild(tasaway);
                                homeStats = tashome.value;
                                awayStats = tasaway.value;
                            }
                            span.appendChild(document.createElement('br'));

                        }
                        let btSave = document.createElement('input');
                        btSave.type = "button";
                        btSave.value = "Speichern";
                        btSave.addEventListener('click', function () {
                            if (game.matchType.toLowerCase().includes("kinder")) {
                                value = {
                                    id: game,
                                    result: null,
                                    homeStats: null,
                                    awayStats: null,
                                    text: ""
                                };
                            } else {
                                value = {
                                    id: game,
                                    result: inpRes.value,
                                    homeStats: tashome.value,
                                    awayStats: tasaway.value,
                                    text: ""
                                };
                                let report = document.getElementById('reportboxes');
                                let taId = value.id.youth;
                                if (!document.getElementById(taId)) {
                                    textArea = document.createElement('textarea');
                                    textArea.id = taId;
                                    textArea.cols = 40;
                                    textArea.rows = 4;
                                    let lb = document.createElement('label');
                                    lb.innerHTML = taId + ":";
                                    report.appendChild(lb);
                                    report.appendChild(document.createElement('br'));
                                    report.appendChild(textArea);
                                    report.appendChild(document.createElement('br'));
                                }
                            }
                            if (saveTemp.length > 0) {
                                for (let i = 0; i < saveTemp.length; i++) {
                                    if (saveTemp[i].id === value.id) {
                                        saveTemp.splice(i, 1);
                                    }
                                }
                            }
                            saveTemp.push(value);
                            showSaveTemp(game);
                        });
                        span.appendChild(btSave);
                        let btCancel = document.createElement('input');
                        btCancel.type = "button";
                        btCancel.value = "Absagen";
                        btCancel.addEventListener('click', function () {
                            value = {id: game, result: null, homeStats: null, awayStats: null, text: "Abgesagt"};
                            if (saveTemp.length > 0) {
                                for (let i = 0; i < saveTemp.length; i++) {
                                    if (saveTemp[i].id === value.id) {
                                        saveTemp.splice(i, 1);
                                    }
                                }
                            }
                            saveTemp.push(value);
                            showSaveTemp(game)
                        });
                        span.appendChild(btCancel);
                        let btDelete = document.createElement('input');
                        btDelete.type = "button";
                        btDelete.value = "Löschen";
                        btDelete.addEventListener('click', function (){
                            deleteMatchEntry("youth", element)
                        });
                        span.appendChild(btDelete);
                    } else {
                        span.textContent = "";
                    }
                });

                match.append(check);
                match.append(matchData);
                match.append(span);

                matchList.append(match);
            });
        })
        .catch((error) => {
            alert("Es ist ein Fehler beim Laden aufgetreten: " + error);
            console.error('Error: ', error);
        });
}

function getMenMatches() {
    let sel = document.getElementById("matches");
    let url = window.location.origin + '/getAllMenMatches'
    fetch(url, {
        method: 'GET'
    })
        .then((result) => result.json())
        .then((data) => {
            data.forEach(element => {
                let match = element.game;
                let type = match.matchType;
                let opp = match.opponent;
                let date = match.matchDate;
                let opt = document.createElement("option");
                opt.text = date + ", " + opp + ", " + type;
                opt.value = JSON.stringify(match);
                sel.append(opt);
            });
        })
        .catch((error) => {
        alert("Es ist ein Fehler beim Laden aufgetreten: " + error);
        console.error('Error: ', error);
    });
}

function deleteMatchEntry(team, element) {
    if (confirm("Soll das Spiel wirklich gelöscht werden?")) {
        let formData = new FormData();
        formData.append("game", JSON.stringify(element.game));
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
