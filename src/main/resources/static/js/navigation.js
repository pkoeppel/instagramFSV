document.addEventListener('DOMContentLoaded', () => {
    const header = document.querySelector('.app-header');
    if (!header) return;

    const links = [
        {id: 'home', label: 'Start', href: '/index.html'},
        {id: 'men-matchday', label: 'Herren Vorschau', href: '/pages/men/matchday-men.html'},
        {id: 'men-lineup', label: 'Herren Aufstellung', href: '/pages/men/lineup-men.html'},
        {id: 'men-halftime', label: 'Herren Halbzeitstand', href: '/pages/men/halftime-men.html'},
        {id: 'men-finish', label: 'Herren Endstand', href: '/pages/men/finish-men.html'},
        {id: 'men-result', label: 'Herren Spielbericht', href: '/pages/men/result-men.html'},
        {id: 'youth-matchday', label: 'Kids Spieltag', href: '/pages/youth/matchday-youth.html'},
        {id: 'youth-result', label: 'Kids Ergebnis', href: '/pages/youth/result-youth.html'},
        {id: 'add-team', label: 'Team anlegen', href: '/pages/addTeam.html'},
        {id: 'settings', label: 'Einstellungen', href: '/pages/changeTemplates.html'},
        {id: 'designer', label: 'Designer', href: '/pages/coordinates.html'}
    ];
    const activePage = header.dataset.active;
    const brand = document.createElement('a');
    brand.className = 'app-brand';
    brand.href = '/index.html';
    brand.textContent = 'Instagram FSV';

    const toggle = document.createElement('button');
    toggle.className = 'nav-toggle';
    toggle.type = 'button';
    toggle.setAttribute('aria-label', 'Navigation öffnen');
    toggle.setAttribute('aria-expanded', 'false');
    toggle.textContent = 'Menü';

    const navigation = document.createElement('nav');
    navigation.className = 'app-navigation';
    navigation.setAttribute('aria-label', 'Hauptnavigation');
    links.forEach(link => {
        const anchor = document.createElement('a');
        anchor.href = link.href;
        anchor.textContent = link.label;
        if (link.id === activePage) {
            anchor.classList.add('active');
            anchor.setAttribute('aria-current', 'page');
        }
        navigation.appendChild(anchor);
    });

    toggle.addEventListener('click', () => {
        const open = navigation.classList.toggle('open');
        toggle.setAttribute('aria-expanded', String(open));
    });

    header.append(brand, toggle, navigation);
});
